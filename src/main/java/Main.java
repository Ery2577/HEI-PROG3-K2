public class Main {
    public static void main(String[] args) {
        DataRetriever dataRetriever = new DataRetriever();

        try {
            // 1. RÉCUPÉRATION d'une commande existante
            // Assure-toi que "ORD-001" existe dans ta table "order"
            Order order = dataRetriever.findOrderByReference("ORD-001");
            System.out.println("Commande récupérée : " + order.getReference() + " | Statut : " + order.getPaymentStatus());

            // 2. TEST : Tentative de création de vente avant paiement (Doit échouer)
            try {
                System.out.println("Tentative de vente sans paiement...");
                dataRetriever.createSaleFrom(order);
            } catch (RuntimeException e) {
                System.out.println("Succès du test d'erreur : " + e.getMessage()); //
            }

            // 3. PAIEMENT de la commande
            order.setPaymentStatus(PaymentStatusEnum.PAID);
            System.out.println("Statut mis à jour : " + order.getPaymentStatus());

            // 4. TEST : Tentative de modification après paiement (Doit échouer)
            try {
                System.out.println("Tentative de modification après paiement...");
                order.setReference("NOUVELLE-REF"); //
            } catch (RuntimeException e) {
                System.out.println("Succès du test de verrouillage : " + e.getMessage());
            }

            // 5. CRÉATION DE LA VENTE
            Sale sale = dataRetriever.createSaleFrom(order); //
            System.out.println("Vente créée avec succès ! ID Vente : " + sale.getId());
            System.out.println("Date de vente : " + sale.getCreationDatetime());

            // 6. TEST : Tentative de vente en double (Doit échouer)
            try {
                dataRetriever.createSaleFrom(order);
            } catch (RuntimeException e) {
                System.out.println("Succès du test d'unicité : " + e.getMessage()); //
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}