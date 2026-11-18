package in.feekeeper.promotion;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PromotionTest {
 @Test void calculatesPercentageAndFlatDiscounts(){
  var tenant=UUID.randomUUID();var today=LocalDate.of(2026,11,18);var amount=new BigDecimal("1000.00");
  var percentage=new Promotion(tenant,"DIWALI25","Diwali",new BigDecimal("25.00"),null,today,today.plusDays(1),null);
  var flat=new Promotion(tenant,"NEWYEAR","New Year",null,new BigDecimal("125.00"),today,today.plusDays(1),365);
  assertEquals(new BigDecimal("250.00"),percentage.discount(amount));
  assertEquals(new BigDecimal("125.00"),flat.discount(amount));
  assertEquals(new BigDecimal("1000.00"),new Promotion(tenant,"ALL","All",null,new BigDecimal("1200.00"),today,today,null).discount(amount));
 }
}
