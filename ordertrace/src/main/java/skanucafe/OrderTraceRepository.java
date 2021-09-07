package skanucafe;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderTraceRepository extends CrudRepository<OrderTrace, Long> {

    List<OrderTrace> findByOrderId(Long orderId);

}