package com.mdframe.forge.starter.idempotent.service;

public interface TokenService {
    
    String generateToken(String prefix);
    
    boolean validateToken(String token, String prefix);
    
    void consumeToken(String token, String prefix);

    /** 原子领取执行许可；validateToken 仅供展示，不能代替本方法。 */
    boolean tryConsumeToken(String token, String prefix);
    
    boolean isTokenConsumed(String token, String prefix);
}
