import com.mongodb.client.*;
import org.bson.Document;

import java.util.*;

public class InternalKnowledgeSharing {

    static class Article {
        int id;
        String title;
        String content;
        List<String> tags;

        public Article(int id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.tags = new ArrayList<>();
        }

        public void addTags(List<String> newTags) {
            tags.addAll(newTags);
        }

        public Document toDocument() {
            return new Document("id", id)
                    .append("title", title)
                    .append("content", content)
                    .append("tags", tags);
        }

        public static Article fromDocument(Document doc) {
            Article a = new Article(doc.getInteger("id"), doc.getString("title"), doc.getString("content"));
            a.tags = (List<String>) doc.get("tags");
            return a;
        }

        @Override
        public String toString() {
            return "\nArticle ID: " + id +
                    "\nTitle: " + title +
                    "\nContent: " + content +
                    "\nTags: " + tags + "\n";
        }
    }

    static class User {
        int id;
        String name;
        List<String> interests;

        public User(int id, String name, List<String> interests) {
            this.id = id;
            this.name = name;
            this.interests = interests;
        }

        public Document toDocument() {
            return new Document("id", id)
                    .append("name", name)
                    .append("interests", interests);
        }

        public static User fromDocument(Document doc) {
            return new User(doc.getInteger("id"), doc.getString("name"), (List<String>) doc.get("interests"));
        }
    }

    static class KnowledgeSharingPlatform {
        Map<Integer, Article> articles = new HashMap<>();
        Map<Integer, User> users = new HashMap<>();

        MongoCollection<Document> articleCollection;
        MongoCollection<Document> userCollection;

        Scanner sc = new Scanner(System.in);

        public KnowledgeSharingPlatform(MongoCollection<Document> articleCollection, MongoCollection<Document> userCollection) {
            this.articleCollection = articleCollection;
            this.userCollection = userCollection;
            loadDataFromDatabase();
        }

        private void loadDataFromDatabase() {
            for (Document doc : articleCollection.find()) {
                Article a = Article.fromDocument(doc);
                articles.put(a.id, a);
            }

            for (Document doc : userCollection.find()) {
                User u = User.fromDocument(doc);
                users.put(u.id, u);
            }
        }

        // Add Article
        public void addArticle() {
            System.out.print("Enter Article ID: ");
            int id = sc.nextInt();
            sc.nextLine();
            if (articles.containsKey(id)) {
                System.out.println("❌ Article with this ID already exists.\n");
                return;
            }

            System.out.print("Enter Title: ");
            String title = sc.nextLine();
            System.out.print("Enter Content: ");
            String content = sc.nextLine();

            Article article = new Article(id, title, content);
            articles.put(id, article);
            articleCollection.insertOne(article.toDocument());

            System.out.println("✅ Article added successfully!\n");
        }

        // View Articles
        public void viewArticles() {
            if (articles.isEmpty()) {
                System.out.println("No articles found.\n");
                return;
            }
            System.out.println("--- All Articles ---");
            for (Article a : articles.values()) {
                System.out.println(a);
            }
        }

        // Delete Article
        public void deleteArticle() {
            System.out.print("Enter Article ID to delete: ");
            int id = sc.nextInt();
            sc.nextLine();
            if (articles.containsKey(id)) {
                articles.remove(id);
                articleCollection.deleteOne(new Document("id", id));
                System.out.println("✅ Article deleted.\n");
            } else {
                System.out.println("❌ Article not found.\n");
            }
        }

        // Tag content
        public void tagContent() {
            System.out.print("Enter Article ID to tag: ");
            int id = sc.nextInt();
            sc.nextLine();
            if (!articles.containsKey(id)) {
                System.out.println("❌ Article not found!\n");
                return;
            }
            System.out.print("Enter tags (comma-separated): ");
            String tagLine = sc.nextLine();
            List<String> tags = Arrays.asList(tagLine.split(","));
            Article article = articles.get(id);
            article.addTags(tags);

            articleCollection.updateOne(
                    new Document("id", id),
                    new Document("$set", new Document("tags", article.tags))
            );
            System.out.println("✅ Tags added successfully!\n");
        }

        // Add user
        public void addUser() {
            System.out.print("Enter User ID: ");
            int id = sc.nextInt();
            sc.nextLine();
            if (users.containsKey(id)) {
                System.out.println("❌ User with this ID already exists.\n");
                return;
            }

            System.out.print("Enter Name: ");
            String name = sc.nextLine();
            System.out.print("Enter interests (comma-separated): ");
            String line = sc.nextLine();
            List<String> interests = Arrays.asList(line.split(","));

            User user = new User(id, name, interests);
            users.put(id, user);
            userCollection.insertOne(user.toDocument());

            System.out.println("✅ User added successfully!\n");
        }

        // Recommend articles
        public void recommendArticles() {
            System.out.print("Enter User ID to recommend articles: ");
            int userId = sc.nextInt();
            sc.nextLine();
            User user = users.get(userId);
            if (user == null) {
                System.out.println("❌ User not found!\n");
                return;
            }

            System.out.println("\n--- Recommended Articles for " + user.name + " ---");
            boolean found = false;
            for (Article a : articles.values()) {
                for (String tag : a.tags) {
                    for (String interest : user.interests) {
                        if (tag.trim().equalsIgnoreCase(interest.trim())) {
                            System.out.println("- " + a.title);
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                System.out.println("No matching articles found.\n");
            } else {
                System.out.println();
            }
        }
    }

    // Main Method
    public static void main(String[] args) {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("knowledge_platform");
        MongoCollection<Document> articleCollection = database.getCollection("articles");
        MongoCollection<Document> userCollection = database.getCollection("users");

        KnowledgeSharingPlatform platform = new KnowledgeSharingPlatform(articleCollection, userCollection);
        Scanner sc = new Scanner(System.in);
        int choice;

        System.out.println("========== INTERNAL KNOWLEDGE SHARING PLATFORM ==========");

        do {
            System.out.println("\n1. Add Article");
            System.out.println("2. View Articles");
            System.out.println("3. Delete Article");
            System.out.println("4. Tag Content");
            System.out.println("5. Add User");
            System.out.println("6. Recommend Articles");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            while (!sc.hasNextInt()) {
                System.out.print("Invalid input. Try again: ");
                sc.next();
            }
            choice = sc.nextInt();

            switch (choice) {
                case 1: platform.addArticle(); break;
                case 2: platform.viewArticles(); break;
                case 3: platform.deleteArticle(); break;
                case 4: platform.tagContent(); break;
                case 5: platform.addUser(); break;
                case 6: platform.recommendArticles(); break;
                case 0: System.out.println("Exiting... Goodbye!"); break;
                default: System.out.println("Invalid choice! Try again.\n");
            }
        } while (choice != 0);

        mongoClient.close();
        sc.close();
    }
}
