/*
 * Copyright 2015 Canoo Engineering AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.canoo.dolphin.server.context;

import com.canoo.dolphin.BeanManager;
import com.canoo.dolphin.impl.BeanBuilderImpl;
import com.canoo.dolphin.impl.BeanManagerImpl;
import com.canoo.dolphin.impl.BeanRepositoryImpl;
import com.canoo.dolphin.impl.ClassRepositoryImpl;
import com.canoo.dolphin.impl.InternalAttributesBean;
import com.canoo.dolphin.impl.PlatformConstants;
import com.canoo.dolphin.impl.PresentationModelBuilderFactory;
import com.canoo.dolphin.impl.collections.ListMapperImpl;
import com.canoo.dolphin.internal.BeanBuilder;
import com.canoo.dolphin.internal.BeanRepository;
import com.canoo.dolphin.internal.ClassRepository;
import com.canoo.dolphin.internal.EventDispatcher;
import com.canoo.dolphin.internal.collections.ListMapper;
import com.canoo.dolphin.server.container.ContainerManager;
import com.canoo.dolphin.server.controller.ControllerHandler;
import com.canoo.dolphin.server.controller.InvokeActionException;
import com.canoo.dolphin.server.event.impl.DolphinContextTaskExecutor;
import com.canoo.dolphin.server.event.impl.DolphinEventBusImpl;
import com.canoo.dolphin.server.impl.ServerControllerActionCallBean;
import com.canoo.dolphin.server.impl.ServerEventDispatcher;
import com.canoo.dolphin.server.impl.ServerPlatformBeanRepository;
import com.canoo.dolphin.server.impl.ServerPresentationModelBuilderFactory;
import org.opendolphin.core.comm.Command;
import org.opendolphin.core.comm.JsonCodec;
import org.opendolphin.core.server.DefaultServerDolphin;
import org.opendolphin.core.server.ServerConnector;
import org.opendolphin.core.server.ServerModelStore;
import org.opendolphin.core.server.action.DolphinServerAction;
import org.opendolphin.core.server.comm.ActionRegistry;
import org.opendolphin.core.server.comm.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class DolphinContext {

    private static final Logger LOG = LoggerFactory.getLogger(DolphinContext.class);

    private final DefaultServerDolphin dolphin;

    private final BeanRepository beanRepository;

    private final BeanManager beanManager;

    private final ControllerHandler controllerHandler;

    private final EventDispatcher dispatcher;

    private ServerPlatformBeanRepository platformBeanRepository;

    private ContainerManager containerManager;

    private String id;

    private DolphinContextTaskExecutor taskExecutor;

    public DolphinContext(ContainerManager containerManager) {
        this.containerManager = containerManager;

        //ID
        id = UUID.randomUUID().toString();

        //Init Open Dolphin
        final ServerModelStore modelStore = new ServerModelStore();
        final ServerConnector serverConnector = new ServerConnector();
        serverConnector.setCodec(new JsonCodec());
        serverConnector.setServerModelStore(modelStore);
        dolphin = new DefaultServerDolphin(modelStore, serverConnector);
        dolphin.registerDefaultActions();

        //Init BeanRepository
        dispatcher = new ServerEventDispatcher(dolphin);
        beanRepository = new BeanRepositoryImpl(dolphin, dispatcher);

        //Init BeanManager
        final PresentationModelBuilderFactory builderFactory = new ServerPresentationModelBuilderFactory(dolphin);
        final ClassRepository classRepository = new ClassRepositoryImpl(dolphin, beanRepository, builderFactory);
        final ListMapper listMapper = new ListMapperImpl(dolphin, classRepository, beanRepository, builderFactory, dispatcher);
        final BeanBuilder beanBuilder = new BeanBuilderImpl(classRepository, beanRepository, listMapper, builderFactory, dispatcher);
        beanManager = new BeanManagerImpl(beanRepository, beanBuilder);

        //Init ControllerHandler
        controllerHandler = new ControllerHandler(containerManager, beanManager);

        //Init TaskExecutor
        taskExecutor = new DolphinContextTaskExecutor();


        //Register commands
        registerDolphinPlatformDefaultCommands();

    }

    private void registerDolphinPlatformDefaultCommands() {
        dolphin.register(new DolphinServerAction() {
            @Override
            public void registerIn(ActionRegistry registry) {

                registry.register(PlatformConstants.REGISTER_CONTROLLER_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        onRegisterController();
                    }
                });

                registry.register(PlatformConstants.DESTROY_CONTROLLER_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        onDestroyController();
                    }
                });

                registry.register(PlatformConstants.CALL_CONTROLLER_ACTION_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        if (platformBeanRepository == null) {
                            throw new IllegalStateException("An action was called before the init-command was sent.");
                        }
                        final ServerControllerActionCallBean bean = platformBeanRepository.getControllerActionCallBean();
                        try {
                            onInvokeControllerAction(bean);
                        } catch (Exception e) {
                            LOG.error("Unexpected exception while invoking action {} on controller {}",
                                    bean.getActionName(), bean.getControllerId(), e);
                            bean.setError(true);
                        }
                    }
                });

                registry.register(PlatformConstants.POLL_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        onPollEventBus();
                    }
                });

                registry.register(PlatformConstants.RELEASE_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        onReleaseEventBus();
                    }
                });

                registry.register(PlatformConstants.INIT_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        //Init PlatformBeanRepository
                        platformBeanRepository = new ServerPlatformBeanRepository(dolphin, beanRepository, dispatcher);
                    }
                });

                registry.register(PlatformConstants.DISCONNECT_COMMAND_NAME, new CommandHandler() {
                    @Override
                    public void handleCommand(Command command, List response) {
                        //Disconnect Client
                        //TODO: How to disconnect?
                    }
                });
            }
        });
    }

    private void onRegisterController() {
        final InternalAttributesBean bean = platformBeanRepository.getInternalAttributesBean();
        String controllerId = controllerHandler.createController(bean.getControllerName());
        bean.setControllerId(controllerId);
        Object model = controllerHandler.getControllerModel(controllerId);
        if (model != null) {
            bean.setModel(model);
        }
    }

    private void onDestroyController() {
        final InternalAttributesBean bean = platformBeanRepository.getInternalAttributesBean();
        controllerHandler.destroyController(bean.getControllerId());
    }

    private void onInvokeControllerAction(ServerControllerActionCallBean bean) throws InvokeActionException {
        controllerHandler.invokeAction(bean);
    }

    private void onPollEventBus() {
        try {
            DolphinEventBusImpl.getInstance().longPoll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void onReleaseEventBus() {
        DolphinEventBusImpl.getInstance().release();
    }

    public DefaultServerDolphin getDolphin() {
        return dolphin;
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public ControllerHandler getControllerHandler() {
        return controllerHandler;
    }

    public ContainerManager getContainerManager() {
        return containerManager;
    }

    public BeanRepository getBeanRepository() {
        return beanRepository;
    }

    @Deprecated
    public DolphinContextTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public String getId() {
        return id;
    }

    public static DolphinContext getCurrentContext() {
        return DolphinContextHandler.getCurrentContext();
    }

    public List<Command> handle(List<Command> commands) {
        List<Command> results = new LinkedList<>();
        for (Command command : commands) {
            results.addAll(dolphin.getServerConnector().receive(command));
        }
        return results;
    }

}
