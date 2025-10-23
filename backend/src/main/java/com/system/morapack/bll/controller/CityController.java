package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.CityAdapter;
import com.system.morapack.schemas.CitySchema;
import com.system.morapack.schemas.Continent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityController {

  private final CityAdapter cityAdapter;

  public CitySchema getCity(Integer id) { return cityAdapter.getCity(id); }

  public List<CitySchema> fetchCities(List<Integer> ids) { return cityAdapter.fetchCities(ids); }

  public CitySchema getByName(String name) { return cityAdapter.getByName(name); }

  public List<CitySchema> getByContinent(Continent continent) { return cityAdapter.getByContinent(continent); }

  public CitySchema createCity(CitySchema request) { return cityAdapter.createCity(request); }

  public List<CitySchema> bulkCreateCities(List<CitySchema> requests) { return cityAdapter.bulkCreateCities(requests); }

  public CitySchema updateCity(Integer id, CitySchema request) { return cityAdapter.updateCity(id, request); }

  public void deleteCity(Integer id) { cityAdapter.deleteCity(id); }

  public void bulkDeleteCities(List<Integer> ids) { cityAdapter.bulkDeleteCities(ids); }
}