create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT');


create table dish
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    dish_type VARCHAR(50),
    price NUMERIC(10, 2) NOT NULL
);

create type ingredient_category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table ingredient
(
    id       serial primary key,
    name     varchar(255),
    price    numeric(10, 2),
    category ingredient_category,
    id_dish  int references dish (id)
);

alter table dish
    add column if not exists price numeric(10, 2);

alter table dish
    rename column price to selling_price;

alter table ingredient
drop column if exists id_dish;

alter table ingredient
    add column if not exists required_quantity numeric(10, 2);

alter table ingredient
drop column if exists required_quantity;

create type unit as enum ('PCS', 'KG', 'L');

create table if not exists dish_ingredient
(
    id                serial primary key,
    id_ingredient     int,
    id_dish           int,
    required_quantity numeric(10, 2),
    unit              unit,
    foreign key (id_ingredient) references ingredient (id),
    foreign key (id_dish) references dish (id)
    );


-- 1. Création de la table 'Order'
CREATE TABLE "order" (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(10) NOT NULL UNIQUE,
    creation_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);



-- 3. Création de la table de liaison 'DishOrder'
CREATE TABLE dish_order (
    id SERIAL PRIMARY KEY,
    id_order INTEGER NOT NULL,
    id_dish INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),

    CONSTRAINT fk_order FOREIGN KEY (id_order) REFERENCES "order"(id) ON DELETE CASCADE,
    CONSTRAINT fk_dish FOREIGN KEY (id_dish) REFERENCES dish(id) ON DELETE CASCADE
);