package in.feekeeper.promotion;

import in.feekeeper.auth.*;
import in.feekeeper.common.exception.BadRequestException;
import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromotionServiceTest {
 private final UUID tenantId=UUID.randomUUID();
 private PromotionRepository repository; private PromotionService service;
 @BeforeEach void setup(){repository=mock(PromotionRepository.class);Clock clock=Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"),ZoneId.of("Asia/Kolkata"));service=new PromotionService(repository,clock);var user=new AuthenticatedUser(UUID.randomUUID(),tenantId,"test@example.com",UserRole.OWNER);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void expiredPromotionIsRejected(){var p=new Promotion(tenantId,"OLD","Old offer",new BigDecimal("10"),null,LocalDate.of(2026,1,1),LocalDate.of(2026,1,31),null);when(repository.findByTenantIdAndCode(tenantId,"OLD")).thenReturn(Optional.of(p));assertThrows(BadRequestException.class,()->service.validate(new in.feekeeper.promotion.dto.PromotionValidationRequest("OLD",new BigDecimal("100"),30)));}
 @Test void looksUpPromotionOnlyInsideAuthenticatedTenant(){assertThrows(NotFoundException.class,()->service.validate(new in.feekeeper.promotion.dto.PromotionValidationRequest("FOREIGN",new BigDecimal("100"),30)));verify(repository).findByTenantIdAndCode(tenantId,"FOREIGN");}
}
