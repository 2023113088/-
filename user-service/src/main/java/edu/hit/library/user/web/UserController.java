package edu.hit.library.user.web;

import edu.hit.library.user.entity.UserAccount;
import edu.hit.library.user.repo.UserRepository;
import edu.hit.library.user.web.dto.RegisterRequest;
import edu.hit.library.user.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserController(UserRepository users) {
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByUsername(req.username().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        UserAccount u = new UserAccount();
        u.setUsername(req.username().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setEmail(req.email() != null ? req.email().trim() : null);
        u.setRole("ADMIN".equalsIgnoreCase(req.role()) ? "ADMIN" : "READER");
        users.save(u);
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return users.findById(id)
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
}
