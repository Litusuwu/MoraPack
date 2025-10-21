package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.Alert;
import com.system.morapack.dao.morapack_psql.repository.AlertRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

  private final AlertRepository alertRepository;

  public Alert getAlert(Integer id) {
    return alertRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Alert not found with id: " + id));
  }

  public List<Alert> fetchAlerts(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return alertRepository.findAll();
    }
    return alertRepository.findByIdIn(ids);
  }

  public List<Alert> getAlertsByStatus(String status) {
    return alertRepository.findByStatus(status);
  }

  public List<Alert> getAlertsByOrder(Integer orderId) {
    return alertRepository.findByOrder_Id(orderId);
  }

  public List<Alert> searchAlerts(String query) {
    return alertRepository.findByDescriptionContainingIgnoreCase(query);
  }

  public Alert createAlert(Alert alert) {
    if (alertRepository.existsByDescriptionAndOrder_Id(alert.getDescription(), alert.getOrder().getId())) {
      throw new IllegalArgumentException("Alert already exists for this order: " + alert.getDescription());
    }
    return alertRepository.save(alert);
  }

  public List<Alert> bulkCreateAlerts(List<Alert> alerts) {
    return alertRepository.saveAll(alerts);
  }

  public Alert updateAlert(Integer id, Alert updates) {
    Alert alert = getAlert(id);

    if (updates.getDescription() != null)
      alert.setDescription(updates.getDescription());
    if (updates.getStatus() != null)
      alert.setStatus(updates.getStatus());

    return alertRepository.save(alert);
  }

  public void deleteAlert(Integer id) {
    if (!alertRepository.existsById(id)) {
      throw new EntityNotFoundException("Alert not found with id: " + id);
    }
    alertRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteAlerts(List<Integer> ids) {
    alertRepository.deleteAllByIdIn(ids);
  }
}
