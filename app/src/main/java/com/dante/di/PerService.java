package com.dante.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * @author flymegoc
 * @date 2018/2/4
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface PerService {
}