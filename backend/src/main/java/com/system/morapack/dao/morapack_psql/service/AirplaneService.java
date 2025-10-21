package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.Airplane;
import com.system.morapack.dao.morapack_psql.repository.AirplaneRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AirplaneService {

  private final AirplaneRepository repository;

  public Airplane get(Integer id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Airplane not found with id: " + id));
  }

  public List<Airplane> fetch(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return repository.findAll();
    return repository.findByIdIn(ids);
  }

  public Airplane create(Airplane a) {
    if (a.getCreatedAt() == null) a.setCreatedAt(LocalDateTime.now());
    normalize(a);
    validate(a);
    if (repository.existsByRegistration(a.getRegistration()))
      throw new IllegalArgumentException("Registration already exists: " + a.getRegistration());
    return repository.save(a);
  }

  public List<Airplane> bulkCreate(List<Airplane> list) {
    list.forEach(a -> { if (a.getCreatedAt() == null) a.setCreatedAt(LocalDateTime.now()); normalize(a); validate(a); });
    return repository.saveAll(list);
  }

  @Transactional
  public Airplane update(Integer id, Airplane patch) {
    Airplane a = get(id);

    if (patch.getRegistration() != null) a.setRegistration(patch.getRegistration());
    if (patch.getModel() != null) a.setModel(patch.getModel());
    if (patch.getCapacity() != null) a.setCapacity(patch.getCapacity());
    if (patch.getStatus() != null) a.setStatus(patch.getStatus());
    if (patch.getCreatedAt() != null) a.setCreatedAt(patch.getCreatedAt());

    normalize(a);
    validate(a);

    repository.findByRegistration(a.getRegistration())
        .filter(existing -> !existing.getId().equals(a.getId()))
        .ifPresent(existing -> { throw new IllegalArgumentException("Registration already exists: " + a.getRegistration()); });

    return repository.save(a);
  }

  public void delete(Integer id) {
    if (!repository.existsById(id)) throw new EntityNotFoundException("Airplane not found with id: " + id);
    repository.deleteById(id);
  }

  @Transactional
  public void bulkDelete(List<Integer> ids) { repository.deleteAllByIdIn(ids); }

  // Queries
  public List<Airplane> getByStatus(String status) { return repository.findByStatus(status); }
  public List<Airplane> getByCapacityAtLeast(Integer min) { return repository.findByCapacityGreaterThanEqual(min); }
  public List<Airplane> getByCreatedAtRange(LocalDateTime start, LocalDateTime end) { return repository.findByCreatedAtBetween(start, end); }

  // Helpers
  private void normalize(Airplane a) {
    if (a.getRegistration() != null) a.setRegistration(a.getRegistration().trim().toUpperCase());
    if (a.getModel() != null) a.setModel(a.getModel().trim());
    if (a.getStatus() != null) a.setStatus(a.getStatus().trim().toUpperCase());
  }

  private void validate(Airplane a) {
    if (a.getRegistration() == null || a.getRegistration().isBlank()) throw new IllegalArgumentException("registration required");
    if (a.getModel() == null || a.getModel().isBlank()) throw new IllegalArgumentException("model required");
    if (a.getCapacity() == null || a.getCapacity() < 0) throw new IllegalArgumentException("capacity invalid");
    if (a.getStatus() == null || a.getStatus().isBlank()) throw new IllegalArgumentException("status required");
    if (a.getCreatedAt() == null) throw new IllegalArgumentException("createdAt required");
  }
}
