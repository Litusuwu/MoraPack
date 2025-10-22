package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.AlertAdapter;
import com.system.morapack.schemas.AlertSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertController {

  private final AlertAdapter alertAdapter;

  public AlertSchema getAlert(Integer id) {
    return alertAdapter.getAlert(id);
  }

  public List<AlertSchema> fetchAlerts(List<Integer> ids) {
    return alertAdapter.fetchAlerts(ids);
  }

  public List<AlertSchema> getAlertsByStatus(String status) {
    return alertAdapter.getAlertsByStatus(status);
  }

  public List<AlertSchema> getAlertsByOrder(Integer orderId) {
    return alertAdapter.getAlertsByOrder(orderId);
  }

  public List<AlertSchema> searchAlerts(String query) {
    return alertAdapter.searchAlerts(query);
  }

  public AlertSchema createAlert(AlertSchema request) {
    return alertAdapter.createAlert(request);
  }

  public List<AlertSchema> bulkCreateAlerts(List<AlertSchema> requests) {
    return alertAdapter.bulkCreateAlerts(requests);
  }

  public AlertSchema updateAlert(Integer id, AlertSchema request) {
    return alertAdapter.updateAlert(id, request);
  }

  public void deleteAlert(Integer id) {
    alertAdapter.deleteAlert(id);
  }

  public void bulkDeleteAlerts(List<Integer> ids) {
    alertAdapter.bulkDeleteAlerts(ids);
  }
}
