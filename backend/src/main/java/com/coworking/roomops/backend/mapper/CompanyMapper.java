package com.coworking.roomops.backend.mapper;

import com.coworking.roomops.backend.domain.Company;
import com.coworking.roomops.backend.model.CompanyResponse;

public final class CompanyMapper {

    private CompanyMapper() {}

    public static CompanyResponse toResponse(Company company) {
        CompanyResponse response =
                new CompanyResponse()
                        .id(company.getId())
                        .nom(company.getNom())
                        .siret(company.getSiret())
                        .adresseFacturation(company.getAdresseFacturation());
        if (company.getTarifHoraire() != null) {
            response.tarifHoraire(company.getTarifHoraire().floatValue());
        }
        return response;
    }
}
