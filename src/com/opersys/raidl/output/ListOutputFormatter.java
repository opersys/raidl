/*
 * Copyright (C) 2014 Opersys inc.
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

package com.opersys.raidl.output;

import com.opersys.raidl.AidlService;

import java.lang.reflect.Method;
import java.util.Set;

public class ListOutputFormatter extends AidlOutputFormatter {

    @Override
    protected String simplifyType(Class<?> clazz, String packageName, Set<String> classImports) {
        return clazz.getCanonicalName();
    }

    @Override
    public void output(boolean withNo, AidlService aidlService) {
        System.out.println("Class: " + aidlService.getServiceClass().getCanonicalName());
        System.out.println();

        for (Integer serviceMethodCode: aidlService.getMethodCodes()) {
            Method serviceMethod;

            serviceMethod = aidlService.getMethodByCode(serviceMethodCode);
            System.out.format("%4d\t%s\n",
                    serviceMethodCode,
                    getMethodOutput(false, null, serviceMethodCode, serviceMethod, null));
        }
    }
}
