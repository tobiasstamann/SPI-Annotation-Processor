package io.toolisticon.spiap.processor.serviceprocessortest;

import io.toolisticon.spiap.processor.serviceprocessortest.TestSpi;
import io.toolisticon.spiap.api.Service;
import io.toolisticon.spiap.api.Services;
import io.toolisticon.spiap.api.OutOfService;

import java.io.InputStream;
import java.lang.NoClassDefFoundError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * A generic service locator.
 */
public class TestSpiServiceLocator {

    /**
     * Caches all loaded services.
     */
    private static List<ServiceImplementation> serviceImplementations;


    /**
     * Exception that is thrown if a specific service implementation can't be found.
     */
     public static class ImplementationNotFoundException extends RuntimeException{

            public ImplementationNotFoundException(String id) {
                super(String.format("Couldn't find implementation for spi TestSpi with id : '%s'",id));
            }

        }

    /**
     * Key class for looking checking available spi service implementations.
     */
    public static class ServiceImplementation {

        private final String id;
        private final String description;
        private final int priority;
        private final boolean outOfService;
        private final TestSpi serviceInstance;

        private ServiceImplementation(TestSpi serviceInstance, String id, String description, Integer priority, boolean outOfService) {
            this.serviceInstance = serviceInstance;
            this.id = id != null ? id : serviceInstance.getClass().getCanonicalName();
            this.description = description != null ? description : "";
            this.priority = priority !=null ? priority : 0;
            this.outOfService = outOfService;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isOutOfService () {
            return outOfService;
        }

        public TestSpi getServiceInstance () {
            return serviceInstance;
        }


    }


    private static ServiceImplementation getServiceImplementationByAnnotations(TestSpi serviceImpl) {

        try {
            Service serviceAnnotation = serviceImpl.getClass().getAnnotation(Service.class);
            if (serviceAnnotation == null) {
                Services servicesAnnotation = serviceImpl.getClass().getAnnotation(Services.class);
                if (servicesAnnotation != null) {
                    for (Service service : servicesAnnotation.value()) {
                        if (TestSpi.class.equals(service.value())) {
                            serviceAnnotation = service;
                            break;
                        }
                    }
                }
            }
            String id = serviceAnnotation != null ? serviceAnnotation.id() : null;
            String description = serviceAnnotation != null  ? serviceAnnotation.description() : null;
            Integer priority = serviceAnnotation != null ? serviceAnnotation.priority() : null;
            boolean outOfService = serviceImpl.getClass().getAnnotation(OutOfService.class) != null;

            return new ServiceImplementation(serviceImpl, id, description, priority, outOfService);

        } catch (NoClassDefFoundError e) {
            // ignore
        }

        return null;

    }

    private static ServiceImplementation getServiceImplementationByProperty(TestSpi serviceImpl) {

        // first try to get config from property file
        final String propertyFileName = "/META-INF/spiap/" + TestSpi.class.getCanonicalName() + "/" + serviceImpl.getClass().getCanonicalName() + ".properties";
        InputStream inputStream = TestSpiServiceLocator.class.getResourceAsStream(propertyFileName);
        if (inputStream != null) {

            try {
                Properties properties = new Properties();
                properties.load(inputStream);

                String id = properties.getProperty("id");
                String description = properties.getProperty("description");
                int priority = Integer.valueOf(properties.getProperty("priority"));
                boolean outOfService= Boolean.valueOf(properties.getProperty("outOfService"));

                return new ServiceImplementation(serviceImpl, id, description, priority, outOfService);

            } catch (Exception e) {
                // do nothing
            }
        }

        return null;

    }


    public static ServiceImplementation getServiceImplementation(TestSpi serviceImpl) {

        // first try to get config from property file
        ServiceImplementation serviceKey = getServiceImplementationByProperty(serviceImpl);

        if (serviceKey != null) {
            return serviceKey;
        }

        // try to get ServiceKey from annotation
        serviceKey = getServiceImplementationByAnnotations(serviceImpl);
        if (serviceKey != null) {
            return serviceKey;
        }

        // return default service key
        return new ServiceImplementation(serviceImpl, null, null, null, false);

    }


   /**
    * Comparator which allows sorting of service implementations by priority.
    */
    public static class ServicePriorityComparator implements Comparator<ServiceImplementation> {

        public int compare (ServiceImplementation o1,ServiceImplementation o2){
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 != null && o2 == null) {
                return -1;
            } else if (o1 == null && o2 != null) {
                 return -1;
            } else {

                if (o1.priority == o2.priority) {
                    return 0;
                } else {
                    return o1.getPriority() < o2.getPriority() ? -1 : 1;
                }

            }
        }

    }

    /*
     * Get {@link ServiceKey} for all available implementations.
     * @return a list that contains ServiceKeys for all available service implementations, or an empty List if none could be found.
     */
    public static List<TestSpiServiceLocator.ServiceImplementation> getServiceImplementations() {

        locateAll();
        return new ArrayList(serviceImplementations);

    }

    /**
     * locate a service implementation with a specific id.
     *
     * @param id the id to search
     * @return the service implementation with the id
     * @throws ImplementationNotFoundException if either passed id is null or if the service implementation can't be found.
     */
    public static TestSpi locateById(String id) {

        if (id != null) {

            locateAll();

            for (ServiceImplementation serviceImpl : serviceImplementations) {

                if (id.equals(serviceImpl.getId())) {
                    return serviceImpl.getServiceInstance();
                }
            }
        }

        throw new ImplementationNotFoundException(id);

    }

    /**
     * Hide constructor.
     */
    private TestSpiServiceLocator() {
    }

    /**
     * Locates a service implementation.
     * The first implementation returned by the locateAll method will be returned if more than one implementation is available.
     * Successive calls may return different service implementations.
     * @return returns the first Implementation found via locateAll method call.
     */
    public static TestSpi locate() {
        final List services = locateAll();
        return services.isEmpty() ? (TestSpi)null : (TestSpi)services.get(0);
    }

    /**
     * Locates all available service implementations.
     * @return returns a list containing all available service implementations or an empty list, if no implementation can be found.
     */
    public static List< TestSpi > locateAll() {

        if (serviceImplementations == null) {

            final Iterator<TestSpi> iterator = ServiceLoader.load(TestSpi.class).iterator();
            final List<ServiceImplementation> tmpServiceImplementations = new ArrayList<ServiceImplementation>();

            while (iterator.hasNext()) {
                try {

                    TestSpi service = iterator.next();

                    ServiceImplementation serviceImpl = getServiceImplementation(service);
                    if (!serviceImpl.isOutOfService()) {
                        tmpServiceImplementations.add(serviceImpl);
                    }

                }
                catch (Error e) {
                    e.printStackTrace(System.err);
                }
            }

            Collections.sort(tmpServiceImplementations, new ServicePriorityComparator());

            serviceImplementations = tmpServiceImplementations;

        }

        // Now return services
        final List<TestSpi> services = new ArrayList<TestSpi>();

        for (ServiceImplementation service : serviceImplementations) {
            services.add(service.getServiceInstance());
        }

        return services;

    }

    /**
     * Clear cache and reload services.
     */
    public static void reloadServices() {
        serviceImplementations = null;
        locateAll();
    }

}
