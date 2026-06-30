package com.nsbm.rocs.entity.main;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
@IdClass(RolePermission.RolePermissionId.class)
public class RolePermission {

    @Id
    @Column(length = 40)
    private String role;

    @Id
    @Column(name = "permission_id")
    private Long permissionId;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RolePermissionId implements Serializable {
        private String role;
        private Long permissionId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RolePermissionId that = (RolePermissionId) o;
            return role.equals(that.role) && permissionId.equals(that.permissionId);
        }

        @Override
        public int hashCode() {
            return 31 * role.hashCode() + permissionId.hashCode();
        }
    }
}

