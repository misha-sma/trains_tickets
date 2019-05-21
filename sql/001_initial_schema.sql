-- типы вагонов
CREATE TABLE IF NOT EXISTS carriage_types (id_carriage_type int, name varchar(256), seats_count int);
ALTER TABLE carriage_types ADD PRIMARY KEY (id_carriage_type);

-- вагоны
CREATE TABLE IF NOT EXISTS carriages (id_carriage bigserial PRIMARY KEY, id_train int, departure_time timestamp without time zone, carriage_number int, id_carriage_type int);
ALTER TABLE carriages ADD CONSTRAINT fk_carriage_type FOREIGN KEY (id_carriage_type) REFERENCES carriage_types(id_carriage_type);

-- поезда
CREATE TABLE IF NOT EXISTS trains (id_train int PRIMARY KEY, name varchar(256), departure_time time without time zone, departure_days varchar(256));
ALTER TABLE carriages ADD CONSTRAINT fk_carriages_trains FOREIGN KEY (id_train) REFERENCES trains(id_train);

-- станции
CREATE TABLE IF NOT EXISTS stations (id_station serial PRIMARY KEY, name varchar(256));

-- поезда-станции
CREATE TABLE IF NOT EXISTS trains_stations (id_train int, id_station int, travel_time int, stay_time int);
ALTER TABLE trains_stations ADD CONSTRAINT fk_trains_stations_trains FOREIGN KEY (id_train) REFERENCES trains(id_train);
ALTER TABLE trains_stations ADD CONSTRAINT fk_trains_stations_stations FOREIGN KEY (id_station) REFERENCES stations(id_station);

-- юзеры
CREATE TABLE IF NOT EXISTS users (id_user bigint, surname varchar(256), name varchar(256), patronymic varchar(256), birthday date, phone bigint, email varchar(256), registration_date timestamp without time zone DEFAULT NOW());
ALTER TABLE users ADD PRIMARY KEY (id_user);

--места
CREATE TABLE IF NOT EXISTS seats (id_seat bigserial PRIMARY KEY, id_carriage bigint, seat_number int, stages varbit 
DEFAULT B'00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000');

ALTER TABLE seats ADD CONSTRAINT fk_seats_carriages FOREIGN KEY (id_carriage) REFERENCES carriages(id_carriage);

-- билеты
CREATE TABLE IF NOT EXISTS tickets (id_ticket bigserial PRIMARY KEY, id_seat bigint, departure_station int, destination_station int, id_user bigint, 
buy_date timestamp without time zone DEFAULT NOW());
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_seats FOREIGN KEY (id_seat) REFERENCES seats(id_seat);
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_stations_departure FOREIGN KEY (departure_station) REFERENCES stations(id_station);
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_stations_destination FOREIGN KEY (destination_station) REFERENCES stations(id_station);
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_users FOREIGN KEY (id_user) REFERENCES users(id_user);
