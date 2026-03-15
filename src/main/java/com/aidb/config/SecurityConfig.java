package com.aidb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/h2-console/**").permitAll()
                .requestMatchers("/admin/**", "/api/stats").hasRole("ADMIN")
                .requestMatchers("/api/csv/**", "/api/**", "/chat", "/").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/chat", true)
                .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("admin")
            .password(encoder.encode("admin123")).roles("ADMIN", "USER").build());
        manager.createUser(User.withUsername("user")
            .password(encoder.encode("user123")).roles("USER").build());
        manager.createUser(User.withUsername("analyst")
            .password(encoder.encode("analyst123")).roles("USER").build());
        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
