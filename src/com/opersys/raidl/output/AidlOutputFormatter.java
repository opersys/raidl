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
import com.opersys.raidl.Utils;

import java.lang.reflect.Method;
import java.util.*;

public class AidlOutputFormatter implements OutputFormatter {

    protected String simplifyType(Class<?> clazz, String packageName, Set<String> classImports) {
        if (clazz.getCanonicalName().startsWith("java.lang") || clazz.getCanonicalName().startsWith(packageName))
            return clazz.getSimpleName();
        else {
            if (!(clazz.isPrimitive() || clazz.isArray()))
                classImports.add(clazz.getCanonicalName());

            return clazz.getSimpleName();
        }
    }

    protected String getMethodOutput(boolean withNo, String packageName, int serviceMethodCode,
                                     Method serviceMethod, Set<String> classImports) {
        String methodParamString;
        Class<?>[] methodParams;
        List<String> methodParamTypes;
        int paramNo = 1;

        methodParams = serviceMethod.getParameterTypes();
        methodParamTypes = new LinkedList<String>();

        for (Class<?> methodParamType : methodParams) {
            String methodParamName;

            if (methodParamType == int.class || methodParamType == long.class)
                methodParamName = "n" + paramNo++;
            else if (methodParamType == String.class)
                methodParamName = "s" + paramNo++;
            else
                methodParamName = "p" + paramNo++;

            methodParamTypes.add(
                    simplifyType(methodParamType, packageName, classImports) + " " + methodParamName);
        }

        methodParamString = Utils.join(methodParamTypes, ", ");

        return simplifyType(serviceMethod.getReturnType(), packageName, classImports)
                + " "
                + serviceMethod.getName()
                + "("
                + methodParamString
                + ");"
                + (withNo ? " // " + serviceMethodCode : "");
    }

    @Override
    public void output(boolean withNo, AidlService aidlService) {
        String packageName, serviceClassName;
        LinkedList<String> outputMethods;
        SortedSet<String> classImports;

        classImports = new TreeSet<String>();
        outputMethods = new LinkedList<String>();

        serviceClassName = aidlService.getServiceClass().getCanonicalName();
        packageName = serviceClassName.substring(0, serviceClassName.lastIndexOf("."));

        for (Integer serviceMethodCode: aidlService.getMethodCodes()) {
            Method serviceMethod;

            serviceMethod = aidlService.getMethodByCode(serviceMethodCode);
            outputMethods.add(getMethodOutput(withNo, packageName, serviceMethodCode, serviceMethod, classImports));
        }

        System.out.println(
                "// Service: "
                + aidlService.getServiceName()
                + ", Interface: " + aidlService.getServiceClass().getCanonicalName());
        System.out.println("package " + packageName + ";\n");

        if (classImports.size() > 0) {
            for (String classImport : classImports)
                System.out.println("import " + classImport + ";");

            System.out.println();
        }

        System.out.println("interface " + aidlService.getServiceClass().getSimpleName() + " {") ;

        System.out.println("    " + Utils.join(outputMethods, "\n    "));

        System.out.println("}");
    }
}
