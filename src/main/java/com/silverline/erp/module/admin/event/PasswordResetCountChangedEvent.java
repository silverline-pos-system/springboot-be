package com.silverline.erp.module.admin.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when the pending password reset request count changes.
 */
public class PasswordResetCountChangedEvent extends ApplicationEvent {

    public PasswordResetCountChangedEvent(Object source) {
        super(source);
    }
}
