/*
 *  /***************************************************************************
 *  Copyright (c) 2017, EPAM SYSTEMS INC
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ***************************************************************************
 */

package com.epam.gmp.process;

import com.epam.gmp.ScriptResult;

import java.util.concurrent.Future;

public interface IQueuedProcessService {
    <R> Future<ScriptResult<R>> execute(IQueuedThread<R> process);

    <R, C extends IQueuedThread<R>> Future<ScriptResult<R>> execute(Class<C> bean, Object... args);

    int getQueueSize();

    int getQueueCapacity();
}
