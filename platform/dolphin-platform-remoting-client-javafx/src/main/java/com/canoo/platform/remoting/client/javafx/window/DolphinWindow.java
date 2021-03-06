/*
 * Copyright 2015-2017 Canoo Engineering AG.
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
package com.canoo.platform.remoting.client.javafx.window;

import com.canoo.platform.remoting.client.javafx.view.AbstractViewController;
import com.canoo.dp.impl.platform.core.Assert;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * A JavaFX {@link Window} that contains the view of a {@link AbstractViewController} and will automatically call
 * {@link AbstractViewController#destroy()} when the stage becomes hidden.
 *
 * @param <M> type of the model
 */
@Deprecated
public class DolphinWindow<M> extends Window {

    /**
     * Constructor
     *
     * @param viewBinder the viewBinder
     */
    public DolphinWindow(final AbstractViewController<M> viewBinder) {
        Assert.requireNonNull(viewBinder, "viewBinder");
        DolphinWindowUtils.destroyOnClose(this, viewBinder);
        setScene(new Scene(viewBinder.getParent()));
    }
}

