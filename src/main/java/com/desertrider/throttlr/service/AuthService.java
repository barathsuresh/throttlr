package com.desertrider.throttlr.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.desertrider.throttlr.dto.request.LoginRequest;
import com.desertrider.throttlr.dto.response.LoginResponse;
import com.desertrider.throttlr.dto.response.RegisterResponse;
import com.desertrider.throttlr.exception.UnauthorizedException;
import com.desertrider.throttlr.model.Account;
import com.desertrider.throttlr.repository.AccountRepository;
import com.desertrider.throttlr.security.jwt.JwtProvider;
import com.desertrider.throttlr.security.service.PassphraseService;
import com.desertrider.throttlr.validation.InputLimits;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final PassphraseService passphraseService;
    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;

    /**
     * Registers new account:
     * 1. Generates cryptographically secure passphrase
     * 2. Creates HMAC lookup (for fast verification) + bcrypt hash (for secure
     * storage)
     * 3. Persists to MongoDB
     * Passphrase shown only once - client must save immediately
     */
    public RegisterResponse register() {
        String passphrase = passphraseService.generatePassphrase();
        String lookup = passphraseService.createLookup(passphrase);
        String hash = passphraseService.hash(passphrase);

        Account account = Account.builder()
                .passphraseHash(hash)
                .passphraseLookup(lookup)
                .createdAt(System.currentTimeMillis())
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("[AUTH] Account registered - accountId: [{}]", savedAccount.getId());

        return new RegisterResponse(
                savedAccount.getId(),
                passphrase,
                "Your passphrase is shown only once. Please store it securely. It cannot be retrieved later.");

    }

    /**
     * Authenticates user:
     * 1. Finds account via lookup hash (fast O(1) lookup)
     * 2. Verifies passphrase against bcrypt hash
     * 3. Generates JWT token for session
     * Throws UnauthorizedException if passphrase invalid
     */
    public LoginResponse login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.passphrase())) {
            throw new IllegalArgumentException("Passphrase is required");
        }
        InputLimits.requireMaxLength(
                request.passphrase(),
                InputLimits.PASSPHRASE_MAX_LENGTH,
                "Passphrase must be at most 300 characters");

        String lookup = passphraseService.createLookup(request.passphrase());

        Account account = accountRepository.findByPassphraseLookup(lookup)
                .orElseThrow(() -> {
                    log.warn("[AUTH] Login failed - account not found");
                    return new UnauthorizedException("Invalid passphrase");
                });

        boolean matches = passphraseService.matches(request.passphrase(), account.getPassphraseHash());
        if (!matches) {
            log.warn("[AUTH] Login failed - invalid passphrase for accountId: [{}]", account.getId());
            throw new UnauthorizedException("Invalid passphrase");
        }

        log.info("[AUTH] Login successful - accountId: [{}]", account.getId());
        String token = jwtProvider.generateToken(account, null);

        return new LoginResponse(token, account.getId());
    }

}
