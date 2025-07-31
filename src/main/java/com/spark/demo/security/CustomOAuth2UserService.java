package com.spark.demo.security;

import com.spark.demo.model.AuthProvider;
import com.spark.demo.model.User;
import com.spark.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId(); // "google" or "facebook"

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = getEmailFromAttributes(registrationId, attributes);
        String name = getNameFromAttributes(registrationId, attributes);
        String imageUrl = getImageFromAttributes(registrationId, attributes);
        String providerId = getProviderIdFromAttributes(registrationId, attributes);

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        User user = userRepository
                .findByEmail(email)
                .orElseGet(() -> registerNewUser(provider, providerId, name, email, imageUrl));

        // Optional: update name or picture if changed
        user.setName(name);
        user.setImageUrl(imageUrl);
        userRepository.save(user);

        return oAuth2User;
    }

    private User registerNewUser(AuthProvider provider, String providerId, String name, String email, String imageUrl) {
        return userRepository.save(User.builder()
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .email(email)
                .imageUrl(imageUrl)
                .emailVerified(true)
                .build());
    }

    private String getEmailFromAttributes(String provider, Map<String, Object> attrs) {
        if (provider.equals("google")) {
            return (String) attrs.get("email");
        } else if (provider.equals("facebook")) {
            return (String) attrs.get("email");
        }
        return null;
    }

    private String getNameFromAttributes(String provider, Map<String, Object> attrs) {
        if (provider.equals("google")) {
            return (String) attrs.get("name");
        } else if (provider.equals("facebook")) {
            return (String) attrs.get("name");
        }
        return "Unknown";
    }

    private String getImageFromAttributes(String provider, Map<String, Object> attrs) {
        if (provider.equals("google")) {
            return (String) attrs.get("picture");
        } else if (provider.equals("facebook")) {
            Map<String, Object> pictureObj = (Map<String, Object>) attrs.get("picture");
            Map<String, Object> data = (Map<String, Object>) pictureObj.get("data");
            return (String) data.get("url");
        }
        return null;
    }

    private String getProviderIdFromAttributes(String provider, Map<String, Object> attrs) {
        return (String) attrs.get("id");
    }
}
