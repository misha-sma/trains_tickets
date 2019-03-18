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
CREATE TABLE IF NOT EXISTS seats (id_seat bigserial PRIMARY KEY, id_carriage bigint, seat_number int, stage_1 bool DEFAULT 'f', stage_2 bool DEFAULT 'f', stage_3 bool DEFAULT 'f', stage_4 bool DEFAULT 'f', stage_5 bool DEFAULT 'f', stage_6 bool DEFAULT 'f', stage_7 bool DEFAULT 'f', stage_8 bool DEFAULT 'f', stage_9 bool DEFAULT 'f', stage_10 bool DEFAULT 'f', stage_11 bool DEFAULT 'f', stage_12 bool DEFAULT 'f', stage_13 bool DEFAULT 'f', stage_14 bool DEFAULT 'f', stage_15 bool DEFAULT 'f', stage_16 bool DEFAULT 'f', stage_17 bool DEFAULT 'f', stage_18 bool DEFAULT 'f', stage_19 bool DEFAULT 'f', stage_20 bool DEFAULT 'f', stage_21 bool DEFAULT 'f', stage_22 bool DEFAULT 'f', stage_23 bool DEFAULT 'f', stage_24 bool DEFAULT 'f', stage_25 bool DEFAULT 'f', stage_26 bool DEFAULT 'f', stage_27 bool DEFAULT 'f', stage_28 bool DEFAULT 'f', stage_29 bool DEFAULT 'f', stage_30 bool DEFAULT 'f', stage_31 bool DEFAULT 'f', stage_32 bool DEFAULT 'f', stage_33 bool DEFAULT 'f', stage_34 bool DEFAULT 'f', stage_35 bool DEFAULT 'f', stage_36 bool DEFAULT 'f', stage_37 bool DEFAULT 'f', stage_38 bool DEFAULT 'f', stage_39 bool DEFAULT 'f', stage_40 bool DEFAULT 'f', stage_41 bool DEFAULT 'f', stage_42 bool DEFAULT 'f', stage_43 bool DEFAULT 'f', stage_44 bool DEFAULT 'f', stage_45 bool DEFAULT 'f', stage_46 bool DEFAULT 'f', stage_47 bool DEFAULT 'f', stage_48 bool DEFAULT 'f', stage_49 bool DEFAULT 'f', stage_50 bool DEFAULT 'f', stage_51 bool DEFAULT 'f', stage_52 bool DEFAULT 'f', stage_53 bool DEFAULT 'f', stage_54 bool DEFAULT 'f', stage_55 bool DEFAULT 'f', stage_56 bool DEFAULT 'f', stage_57 bool DEFAULT 'f', stage_58 bool DEFAULT 'f', stage_59 bool DEFAULT 'f', stage_60 bool DEFAULT 'f', stage_61 bool DEFAULT 'f', stage_62 bool DEFAULT 'f', stage_63 bool DEFAULT 'f', stage_64 bool DEFAULT 'f', stage_65 bool DEFAULT 'f', stage_66 bool DEFAULT 'f', stage_67 bool DEFAULT 'f', stage_68 bool DEFAULT 'f', stage_69 bool DEFAULT 'f', stage_70 bool DEFAULT 'f', stage_71 bool DEFAULT 'f', stage_72 bool DEFAULT 'f', stage_73 bool DEFAULT 'f', stage_74 bool DEFAULT 'f', stage_75 bool DEFAULT 'f', stage_76 bool DEFAULT 'f', stage_77 bool DEFAULT 'f', stage_78 bool DEFAULT 'f', stage_79 bool DEFAULT 'f', stage_80 bool DEFAULT 'f', stage_81 bool DEFAULT 'f', stage_82 bool DEFAULT 'f', stage_83 bool DEFAULT 'f', stage_84 bool DEFAULT 'f', stage_85 bool DEFAULT 'f', stage_86 bool DEFAULT 'f', stage_87 bool DEFAULT 'f', stage_88 bool DEFAULT 'f', stage_89 bool DEFAULT 'f', stage_90 bool DEFAULT 'f', stage_91 bool DEFAULT 'f', stage_92 bool DEFAULT 'f', stage_93 bool DEFAULT 'f', stage_94 bool DEFAULT 'f', stage_95 bool DEFAULT 'f', stage_96 bool DEFAULT 'f', stage_97 bool DEFAULT 'f', stage_98 bool DEFAULT 'f', stage_99 bool DEFAULT 'f', stage_100 bool DEFAULT 'f', stage_101 bool DEFAULT 'f', stage_102 bool DEFAULT 'f', stage_103 bool DEFAULT 'f', stage_104 bool DEFAULT 'f', stage_105 bool DEFAULT 'f', stage_106 bool DEFAULT 'f', stage_107 bool DEFAULT 'f', stage_108 bool DEFAULT 'f', stage_109 bool DEFAULT 'f', stage_110 bool DEFAULT 'f', stage_111 bool DEFAULT 'f', stage_112 bool DEFAULT 'f', stage_113 bool DEFAULT 'f', stage_114 bool DEFAULT 'f', stage_115 bool DEFAULT 'f', stage_116 bool DEFAULT 'f', stage_117 bool DEFAULT 'f', stage_118 bool DEFAULT 'f', stage_119 bool DEFAULT 'f', stage_120 bool DEFAULT 'f', stage_121 bool DEFAULT 'f', stage_122 bool DEFAULT 'f', stage_123 bool DEFAULT 'f', stage_124 bool DEFAULT 'f', stage_125 bool DEFAULT 'f', stage_126 bool DEFAULT 'f', stage_127 bool DEFAULT 'f', stage_128 bool DEFAULT 'f', stage_129 bool DEFAULT 'f', stage_130 bool DEFAULT 'f', stage_131 bool DEFAULT 'f', stage_132 bool DEFAULT 'f', stage_133 bool DEFAULT 'f', stage_134 bool DEFAULT 'f', stage_135 bool DEFAULT 'f', stage_136 bool DEFAULT 'f', stage_137 bool DEFAULT 'f', stage_138 bool DEFAULT 'f', stage_139 bool DEFAULT 'f', stage_140 bool DEFAULT 'f', stage_141 bool DEFAULT 'f', stage_142 bool DEFAULT 'f', stage_143 bool DEFAULT 'f', stage_144 bool DEFAULT 'f', stage_145 bool DEFAULT 'f', stage_146 bool DEFAULT 'f', stage_147 bool DEFAULT 'f', stage_148 bool DEFAULT 'f', stage_149 bool DEFAULT 'f', stage_150 bool DEFAULT 'f');

ALTER TABLE seats ADD CONSTRAINT fk_seats_carriages FOREIGN KEY (id_carriage) REFERENCES carriages(id_carriage);

-- билеты
CREATE TABLE IF NOT EXISTS tickets (id_ticket bigserial PRIMARY KEY, id_seat bigint, departure_station int, destination_station int, id_user bigint, 
buy_date timestamp without time zone DEFAULT NOW());
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_seats FOREIGN KEY (id_seat) REFERENCES seats(id_seat);
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_stations_departure FOREIGN KEY (departure_station) REFERENCES stations(id_station);
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_stations_destination FOREIGN KEY (destination_station) REFERENCES stations(id_station);
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_users FOREIGN KEY (id_user) REFERENCES users(id_user);
