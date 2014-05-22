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

package com.opersys.raidl;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.opersys.raidl.output.AidlOutputFormatter;
import com.opersys.raidl.output.ListOutputFormatter;
import com.opersys.raidl.output.OutputFormatter;
import com.opersys.raidl.output.SingleAidlOutputFormatter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class Raidl {

    private static String TAG = "Raidl";

    private static int listServices() {
        try {
            IBinder serviceBinder;
            String serviceInterface;

            for (String serviceName : ServiceManager.listServices()) {
                serviceBinder = ServiceManager.getService(serviceName);
                serviceInterface = serviceBinder.getInterfaceDescriptor();

                if (!serviceInterface.equals(""))
                    System.out.println(serviceName + ": " + serviceInterface);
                else
                    System.out.println(serviceName + ": No Interface");
            }

            return 0;

        } catch (RemoteException ex) {
            System.err.println("Error listing services");
            ex.printStackTrace(System.err);
            return 1;
        }
    }

    private static int showUsage() {
        System.out.println("Reverse AIDL tool: version 0.9.0");
        System.out.println("Copyright (C) 2014, Opersys inc. -- www.opersys.com");
        System.out.println();
        System.out.println("Usage: raidl");
        System.out.println("       raidl list");
        System.out.println("       raidl iface [-l, -n] SERVICE [METHOD | CODE]");
        System.out.println("          (-l: dump as simple list, -n: add transaction codes)");

        return 0;
    }

    private static boolean isRemoteMethod(Method method) {
        boolean isRemoteMethod = false;

        for (Class<?> methodException : method.getExceptionTypes()) {
            if (methodException == RemoteException.class) {
                isRemoteMethod = true;
                break;
            }
        }

        return isRemoteMethod;
    }

    private static Class tryloadServiceClass(String serviceClassName) throws ClassNotFoundException {
        Class serviceClass = null;
        int idx = 0;

        // This is a list of namespaces in which to search for service that might return a simplified
        // interface name instead of a canonical name.
        String[] androidNamespacesPrefixes = {
                "",
                "android.os.",
                "android.os.storage.",
                "android.service.",
                "android.service.notification.",
                "android.service.textservice.",
                "android.accessibilityservice"
        };

        while (serviceClass == null) {
            String augmentedInterfaceName;

            if (idx == androidNamespacesPrefixes.length)
                throw new ClassNotFoundException("Class not found for "
                        + serviceClassName
                        + " (C++ services not supported)");

            augmentedInterfaceName = androidNamespacesPrefixes[idx++] + serviceClassName;

            try {
                serviceClass = Raidl.class.getClassLoader().loadClass(augmentedInterfaceName);
            } catch (ClassNotFoundException ex) {
                serviceClass = null;
            }
        }

        return serviceClass;
    }

    private static boolean looksLikeTransactionCode(String transactionCodeName) {
        return transactionCodeName.startsWith("TRANSACTION_") || transactionCodeName.endsWith("_TRANSACTION");
    }

    private static String getMethodNameForTransaction(String serviceName, String transactionCodeName) {
        // This is a list of methods in IActivityManager for which the service code doesn't
        // quite give us the method name after our simple transformation.
        String[][] activityServiceQuirks = {
                { "clearAppData", "clearApplicationUserData" }, // 4.1
                { "getDeviceConfiguration", "getDeviceConfigurationInfo" }, // 4.1
                { "startBackupAgent", "bindBackupAgent" }, // 4.1
                { "killApplicationWithAppid", "killApplicationWithAppId" }, // 4.4
                { "resizeStack", "resizeStackBox" }, // 4.4
        };

        if (transactionCodeName.startsWith("TRANSACTION_")) {
            return transactionCodeName.replace("TRANSACTION_", "");
        }
        // This is to handle transaction codes in the style of IActivityManager.java
        else if (transactionCodeName.endsWith("_TRANSACTION")) {
            String[] transactMethNameParts = transactionCodeName.replace("_TRANSACTION", "").split("_");
            String transactMethName = "";

            for (String namePart : transactMethNameParts) {
                if (transactMethName.equals(""))
                    transactMethName += namePart.toLowerCase();
                else
                    transactMethName += namePart.substring(0, 1) + namePart.substring(1).toLowerCase();
            }

            if (serviceName.equals("activity")) {
                for (String[] quirk : activityServiceQuirks) {
                    if (quirk[0].equals(transactMethName))
                        return quirk[1];
                }
            }

            return transactMethName;
        }

        throw new IllegalArgumentException(
                "Codename doesn't look like a transaction code constant: " + transactionCodeName);
    }

    private static int reverseAidl(boolean showCodes, boolean dumpOnly,
                                   String serviceName, String desiredMethodName, Integer desiredMethodCode) {
        IBinder serviceBinder;
        String serviceClassName;
        Class<?> serviceClass = null, serviceStubClass;
        SortedMap<Integer, String> serviceCodesMethods;
        Map<String, Method> serviceMethods;
        boolean singleDisplay = false;
        AidlService aidlService;
        OutputFormatter outputFormatter;

        // Determine if we output a full AIDL or just the signature of one method.

        if (desiredMethodName != null || desiredMethodCode != null)
            singleDisplay = true;

        if (dumpOnly)
            outputFormatter = new ListOutputFormatter();
        else {
            if (singleDisplay)
                outputFormatter = new SingleAidlOutputFormatter();
            else
                outputFormatter = new AidlOutputFormatter();
        }

        serviceBinder = ServiceManager.getService(serviceName);

        if (serviceBinder == null) {
            System.err.println("Unable to get service: " + serviceName);
            return 1;
        }

        try {
            serviceClassName = serviceBinder.getInterfaceDescriptor();

            if (serviceClassName.equals("")) {
                System.err.println("No interface descriptor returned for service: '" + serviceName + "'");
                return 1;
            }

            serviceClass = tryloadServiceClass(serviceClassName);

            if (!serviceName.equals("activity"))
                serviceStubClass = Raidl.class.getClassLoader().loadClass(serviceClass.getCanonicalName() + "$Stub");
            else
                serviceStubClass = serviceClass;

            aidlService = new AidlService(serviceName, serviceClass);

            serviceCodesMethods = new TreeMap<Integer, String>();

            // Get the transaction codes.
            for (Field serviceField : serviceStubClass.getDeclaredFields()) {
                int serviceFieldValue;
                String methodName;

                if (serviceField.getType() == int.class && looksLikeTransactionCode(serviceField.getName())) {
                    serviceField.setAccessible(true);
                    serviceFieldValue = serviceField.getInt(null);
                    methodName = getMethodNameForTransaction(serviceName, serviceField.getName());
                    serviceCodesMethods.put(serviceFieldValue, methodName);
                }
            }

            serviceMethods = new HashMap<String, Method>();

            // Get the methods by name.
            for (Method serviceMethod : serviceClass.getMethods())
                serviceMethods.put(serviceMethod.getName(), serviceMethod);

            for (Integer serviceCode : serviceCodesMethods.keySet()) {
                Method serviceMethod;
                String serviceCodeMethodName;

                serviceCodeMethodName = serviceCodesMethods.get(serviceCode);

                // Examine just what the user passed as command line argument.

                if (desiredMethodCode != null && !serviceCode.equals(desiredMethodCode))
                    continue;

                if (desiredMethodName != null && !desiredMethodName.equals(serviceCodeMethodName))
                    continue;

                serviceMethod = serviceMethods.get(serviceCodeMethodName);

                if (isRemoteMethod(serviceMethod))
                    aidlService.addMethod(serviceCode, serviceMethod);
            }

            if (singleDisplay && aidlService.getMethodCodes().length == 0) {
                if (desiredMethodCode != null) {
                    System.err.println("Could not find method code "
                            + desiredMethodCode
                            + " in service '"
                            + serviceName + "'");
                    return 1;
                }
                if (desiredMethodName != null) {
                    System.err.println("Could not find method named '"
                            + desiredMethodName
                            + "' in service '"
                            + serviceName + "'");
                    return 1;
                }
            }

            outputFormatter.output(showCodes, aidlService);

        } catch (RemoteException e) {
            String s = "Error communicating with Binder";
            System.err.println(s);
            Log.e(TAG, s, e);

        } catch (ClassNotFoundException e) {
            String s = "Failed to load class for service '" + serviceName + " (C++ services not supported)'";
            System.err.println(s);
            Log.e(TAG, s, e);

        } catch (IllegalAccessException e) {
            String s = "Illegal access exception for service '" + serviceName + "'";
            System.err.println(s);
            Log.e(TAG, s, e);
        }

        return 0;
    }

    public static void main(String[] args) {
        CommandLine cmdLine;
        String serviceName, methodName = null;
        Integer methodCode = null;

        if (args.length == 0)
            System.exit(showUsage());

        try {
            if (args[0].equals("list"))
                System.exit(listServices());

            else if (args[0].equals("iface")) {
                cmdLine = new CommandLine(Arrays.copyOfRange(args, 1, args.length), "-n", "-l");

                if (cmdLine.getArgs().length == 0)
                    System.exit(showUsage());

                serviceName = cmdLine.getArgs()[0];

                if (cmdLine.getArgs().length == 2) {
                    try {
                        methodCode = Integer.parseInt(cmdLine.getArgs()[1]);
                    } catch (NumberFormatException ignored) {
                        methodName = cmdLine.getArgs()[1];
                    }
                }

                boolean showCodes = cmdLine.hasOption("-n");
                boolean dumpOnly = cmdLine.hasOption("-l");

                System.exit(reverseAidl(showCodes, dumpOnly, serviceName, methodName, methodCode));
            }
            else System.exit(showUsage());


        } catch (CommandLineException e) {
            System.err.println(e.getMessage());
        }

        System.exit(0);
    }
}
