ALTER TABLE Dish ADD COLUMN IF NOT EXISTS selling_price NUMERIC(10, 2) DEFAULT NULL;

-- Création du type ENUM pour les unités (PCS, KG, L)
DO $$ BEGIN
CREATE TYPE unit_type AS ENUM ('PCS', 'KG', 'L');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Création de la table de jointure DishIngredient
-- Cette table gère la relation ManyToMany et stocke la quantité requise
CREATE TABLE IF NOT EXISTS DishIngredient (
    id SERIAL PRIMARY KEY,
    id_dish INT REFERENCES Dish(id) ON DELETE CASCADE,
    id_ingredient INT REFERENCES Ingredient(id) ON DELETE CASCADE,
    quantity_required NUMERIC(10, 2) NOT NULL,
    unit unit_type NOT NULL
    );

-- Normalisation de la table Ingredient : suppression du lien direct id_dish
ALTER TABLE Ingredient DROP COLUMN IF EXISTS id_dish;


--- 2. INSERTION DES DONNÉES DE TEST ---
INSERT INTO DishIngredient (id, id_dish, id_ingredient, quantity_required, unit) VALUES
    (1, 1, 1, 0.20, 'KG'),
    (2, 1, 2, 0.15, 'KG'),
    (3, 2, 3, 1.00, 'KG'),
    (4, 4, 4, 0.30, 'KG'),
    (5, 4, 5, 0.20, 'KG');


UPDATE Dish SET selling_price = 3500.00 WHERE id = 1;
UPDATE Dish SET selling_price = 12000.00 WHERE id = 2;
UPDATE Dish SET selling_price = NULL WHERE id = 3;
UPDATE Dish SET selling_price = 8000.00 WHERE id = 4;
UPDATE Dish SET selling_price = NULL WHERE id = 5;