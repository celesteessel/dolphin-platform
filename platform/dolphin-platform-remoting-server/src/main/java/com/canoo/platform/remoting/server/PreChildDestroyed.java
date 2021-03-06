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
package com.canoo.platform.remoting.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Can you used to annotate a method in a dolphin controller (see {@link DolphinController}) as an event handler that will automatically be called if a child controller of the given controller will be destroyed (see client API).
 * The method must have exactly one parameter that is of the class or superclass / interface of the child controller.
 *
 * @author Hendrik Ebbers
 *
 * @see DolphinController
 * @see ParentController
 * @see PostChildCreated
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface PreChildDestroyed {
}
