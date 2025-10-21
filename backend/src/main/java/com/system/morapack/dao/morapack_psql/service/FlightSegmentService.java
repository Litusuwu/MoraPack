package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.FlightSegment;
import com.system.morapack.dao.morapack_psql.repository.FlightSegmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class FlightSegmentService {

  private final FlightSegmentRepository flightSegmentRepository;

  public FlightSegment getFlightSegment(Integer id) {
    return flightSegmentRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("FlightSchema segment not found with id: " + id));
  }

  public List<FlightSegment> fetchFlightSegments(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return flightSegmentRepository.findAll();
    }
    return flightSegmentRepository.findByIdIn(ids);
  }

  public List<FlightSegment> getByTravelPlan(Integer planId) {
    return flightSegmentRepository.findByTravelPlan_Id(planId);
  }

  public List<FlightSegment> getByOrder(Integer orderId) {
    return flightSegmentRepository.findByOrder_Id(orderId);
  }

  public List<FlightSegment> getByCreationRange(LocalDateTime start, LocalDateTime end) {
    return flightSegmentRepository.findByCreatedAtBetween(start, end);
  }

  public FlightSegment createFlightSegment(FlightSegment flightSegment) {
    return flightSegmentRepository.save(flightSegment);
  }

  public List<FlightSegment> bulkCreateFlightSegments(List<FlightSegment> flightSegments) {
    return flightSegmentRepository.saveAll(flightSegments);
  }

  public FlightSegment updateFlightSegment(Integer id, FlightSegment updates) {
    FlightSegment segment = getFlightSegment(id);

    if (updates.getEstimateTimeDestination() != null)
      segment.setEstimateTimeDestination(updates.getEstimateTimeDestination());
    if (updates.getEstimatedTimeArrival() != null)
      segment.setEstimatedTimeArrival(updates.getEstimatedTimeArrival());
    if (updates.getReservedCapacity() != null)
      segment.setReservedCapacity(updates.getReservedCapacity());

    return flightSegmentRepository.save(segment);
  }

  public void deleteFlightSegment(Integer id) {
    if (!flightSegmentRepository.existsById(id)) {
      throw new EntityNotFoundException("FlightSchema segment not found with id: " + id);
    }
    flightSegmentRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteFlightSegments(List<Integer> ids) {
    flightSegmentRepository.deleteAllByIdIn(ids);
  }
}
