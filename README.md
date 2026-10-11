# AppLens

docker run -d \
  --name applens-postgres \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=applens \
  postgres:14

docker ps


docker exec -it applens-postgres psql -U postgres -d applens


docker stop applens-postgres


docker start applens-postgres