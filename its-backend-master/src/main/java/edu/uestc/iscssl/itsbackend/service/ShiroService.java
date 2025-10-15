package edu.uestc.iscssl.itsbackend.service;

import java.util.Set;

public interface ShiroService {
    Set<String> getUserPermissions(long userId);
}
