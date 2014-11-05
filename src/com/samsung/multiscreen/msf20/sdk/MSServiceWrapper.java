package com.samsung.multiscreen.msf20.sdk;

import com.samsung.multiscreen.service.MSService;

/**
 * @author plin
 *
 * Simply wraps a {@link MSService} object.
 * 
 */
public class MSServiceWrapper  {
    private MSService service = null;

    public MSServiceWrapper() {
    }
    
    public MSServiceWrapper(MSService service) {
        this.service = service;
    }

    public MSService getService() {
        return service;
    }

    public void setService(MSService service) {
        this.service = service;
    }

    @Override
    public boolean equals(Object object) {
        if ((object != null) && 
                (object instanceof MSServiceWrapper)) {
            MSService s1 = getService();
            MSService s2 = ((MSServiceWrapper)object).getService();

            if (((s1 == null) && (s2 != null)) || 
                ((s1 != null) && (s2 == null))) {
                return false;
            }
            return ((s1 == s2) || 
                    s1.getEndPoint().equals(s2.getEndPoint()) || 
                    (s1.getId().equals(s2.getId()) && 
                    s1.getDeviceName().equals(s2.getDeviceName())));
        }
        return false;
    }

}
