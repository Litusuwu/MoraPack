set-up-db:
	docker compose down && \
	docker compose up morapack-postgres -d --wait
	./mvnw -q -DskipTests \
  -Dspring-boot.run.main-class=com.system.morapack.dao.morapack_psql.DbSetupApplication \
  spring-boot:run
