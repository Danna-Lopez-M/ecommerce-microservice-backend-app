package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.selimhorri.app.domain.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {
	
	@Query("SELECT DISTINCT a FROM Address a JOIN FETCH a.user WHERE a.addressId = :addressId")
	Optional<Address> findByIdWithUser(@Param("addressId") Integer addressId);
	
	@Query("SELECT DISTINCT a FROM Address a JOIN FETCH a.user")
	List<Address> findAllWithUser();
	
}
