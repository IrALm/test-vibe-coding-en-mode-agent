package com.walsia.compta.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class KeycloakRealmRoleGrantedAuthoritiesConverterTest {

    private final KeycloakRealmRoleGrantedAuthoritiesConverter converter = new KeycloakRealmRoleGrantedAuthoritiesConverter();

    @Mock
    private Jwt jwt;

    @Test
    void convert_mapsRealmAccessRolesToPrefixedAuthorities() {
        when(jwt.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("SUPER_ADMIN", "USER")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_USER");
    }

    @Test
    void convert_returnsEmptyCollectionWhenRealmAccessClaimAbsent() {
        when(jwt.getClaimAsMap("realm_access")).thenReturn(null);

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void convert_returnsEmptyCollectionWhenRolesEntryMissing() {
        when(jwt.getClaimAsMap("realm_access")).thenReturn(Map.of("other", "value"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }
}
