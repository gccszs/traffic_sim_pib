package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.service.ShiroService;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ShiroServiceImpl implements ShiroService {


    @Override
    public Set<String> getUserPermissions(long userId) {
        //todo
        return null;
    }
}
