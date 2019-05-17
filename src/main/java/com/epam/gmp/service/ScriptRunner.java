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

package com.epam.gmp.service;

import com.epam.gmp.ExportBinding;
import com.epam.gmp.ScriptResult;
import com.epam.gmp.process.GroovyThread;
import com.epam.gmp.process.QueuedProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@ExportBinding
@Service("ScriptRunner")
public class ScriptRunner {
    @Autowired
    QueuedProcessService processService;

    public <R> Future<ScriptResult<R>> run(String name, Object params) {
        return processService.execute(GroovyThread.class, name, params);
    }
}
