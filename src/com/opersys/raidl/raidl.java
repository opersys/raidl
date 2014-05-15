package com.opersys.raidl;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.*;

public class raidl {

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

    private static String join(Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();

        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }

        return builder.toString();
    }

    private static String simplifyType(Class<?> clazz, String packageName, Set<String> classImports) {
        if (clazz.getCanonicalName().startsWith("java.lang") || clazz.getCanonicalName().startsWith(packageName))
            return clazz.getSimpleName();
        else {
            if (!(clazz.isPrimitive() || clazz.isArray()))
                classImports.add(clazz.getCanonicalName());

            return clazz.getSimpleName();
        }
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

    private static int reverseAidl(String serviceName) {
        IBinder serviceBinder;
        String serviceClassName, packageName;
        Class<?> serviceClass, serviceStubClass;
        SortedMap<Integer, String> serviceCodesMethods;
        LinkedList<String> aidlMethods;
        Map<String, Method> serviceMethods;
        SortedSet<String> classImports;

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

            serviceClass = raidl.class.getClassLoader().loadClass(serviceClassName);
            serviceStubClass = raidl.class.getClassLoader().loadClass(serviceClassName + "$Stub");

            System.out.println("// Service: " + serviceName
                    + ", Interface: " + serviceClassName
                    + ", Stub: " + serviceStubClass.getCanonicalName());

            packageName = serviceClassName.substring(0, serviceClassName.lastIndexOf("."));
            //serviceClassName = serviceClassName.substring(serviceClassName.lastIndexOf(".") + 1);

            serviceCodesMethods = new TreeMap<Integer, String>();

            // Get the transaction codes.
            for (Field serviceField : serviceStubClass.getDeclaredFields()) {
                int serviceFieldValue;

                if (serviceField.getType() == int.class && serviceField.getName().startsWith("TRANSACTION_")) {
                    serviceField.setAccessible(true);
                    serviceFieldValue = serviceField.getInt(null);
                    serviceCodesMethods.put(serviceFieldValue, serviceField.getName().replace("TRANSACTION_", ""));
                }
            }

            serviceMethods = new HashMap<String, Method>();

            // Get the methods by name.
            for (Method serviceMethod : serviceClass.getMethods())
                serviceMethods.put(serviceMethod.getName(), serviceMethod);

            aidlMethods = new LinkedList<String>();
            classImports = new TreeSet<String>();

            for (Integer serviceCode : serviceCodesMethods.keySet()) {
                Class<?> methodReturnType;
                Class<?>[] methodParams;
                Method serviceMethod;
                List<String> methodParamTypes;
                String methodParamString, serviceCodeMethodName;
                int paramNo = 1;

                serviceCodeMethodName = serviceCodesMethods.get(serviceCode);
                serviceMethod = serviceMethods.get(serviceCodeMethodName);
                methodParamTypes = new LinkedList<String>();

                if (isRemoteMethod(serviceMethod)) {
                    methodReturnType = serviceMethod.getReturnType();
                    methodParams = serviceMethod.getParameterTypes();

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

                    methodParamString = join(methodParamTypes, ", ");

                    aidlMethods.add(simplifyType(methodReturnType, packageName, classImports)
                            + " "
                            + serviceMethod.getName()
                            + "("
                            + methodParamString
                            + ");\n");
                }
            }

            System.out.println("package " + packageName + ";\n");

            if (classImports.size() != 0) {
                for (String classImport : classImports)
                    System.out.println("import " + classImport + ";");
                System.out.println("\n");
            }

            System.out.println("interface " + serviceClass.getSimpleName() + " {") ;

            for (String aidlMethod : aidlMethods)
                System.out.println("    " + aidlMethod);

            System.out.println("}");

        } catch (RemoteException e) {
            e.printStackTrace();

        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load class for service '" + serviceName + "'");
            e.printStackTrace(System.err);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Arguments: -l | service_name");
        }

        if (args[0].equals("-l"))
            System.exit(listServices());
        else
            System.exit(reverseAidl(args[0]));
    }
}
