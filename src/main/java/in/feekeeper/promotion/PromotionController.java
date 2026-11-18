package in.feekeeper.promotion;
import in.feekeeper.promotion.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/promotions") @SecurityRequirement(name="bearerAuth")
public class PromotionController {
 private final PromotionService service; public PromotionController(PromotionService service){this.service=service;}
 @GetMapping public List<PromotionResponse> list(){return service.list();}
 @PostMapping public PromotionResponse create(@Valid @RequestBody PromotionRequest request){return service.create(request);}
 @PostMapping("/validate") public PromotionResponse validate(@Valid @RequestBody PromotionValidationRequest request){return service.validate(request);}
}
