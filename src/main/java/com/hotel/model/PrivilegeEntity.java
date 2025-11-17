package com.hotel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "privileges")
public class PrivilegeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String code;
    
    @Column(nullable = false, length = 255)
    private String description;
    
    public PrivilegeEntity() {
    }
    
    public PrivilegeEntity(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrivilegeEntity that = (PrivilegeEntity) o;
        return code != null && code.equals(that.code);
    }
    
    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}

