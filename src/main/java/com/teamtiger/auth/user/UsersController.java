package com.teamtiger.auth.user;

import com.teamtiger.auth.auth.dto.MeResponse;
import com.teamtiger.auth.auth.dto.ProfileUpdateRequest;
import com.teamtiger.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// user/UsersController.java  (프로필 조회/수정)
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {
    private final UserRepository userRepo;

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal User user) {
        return new MeResponse(
                user.getId(), user.getLoginId(), user.getEmail(), user.getRole(),
                user.getName(), user.getGender()==null?null:user.getGender().name(),
                user.getBirthDate()==null?null:user.getBirthDate().toString(),
                user.getNickname(), user.getPreferredTheme(), user.getHomeLocation()
        );
    }

    @PatchMapping("/me")
    public MeResponse update(@AuthenticationPrincipal User user, @RequestBody ProfileUpdateRequest req) {
        if (req.nickname()!=null) user.setNickname(req.nickname());
        if (req.preferredTheme()!=null) user.setPreferredTheme(req.preferredTheme());
        if (req.homeLocation()!=null) user.setHomeLocation(req.homeLocation());
        userRepo.save(user);
        return me(user);
    }
}
