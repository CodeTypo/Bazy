package services;

import entities.Customer;
import entities.Order;
import entities.Paczkamat;
import entities.Stash;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa DBService odpowiadająca za nawiązanie połączenia z bazą danych online przy pomocy Hibernate,
 * Posiada metody typu get/insert pozwalające na zarządzanie danymi zebranymi w bazie.
 */
public class DBService implements DataService {
    private static SessionFactory factory;
    private static Session session;
    private static Transaction tx = null;

    public DBService(String username, String password) {
        try {
            Configuration cfg = new Configuration();
            cfg.configure("hibernate/hibernate.cfg.xml"); //hibernate config xml file name
            cfg.getProperties().setProperty("hibernate.connection.password", password);
            cfg.getProperties().setProperty("hibernate.connection.username", username);
            factory = cfg.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }


    public List<Paczkamat> getAllPaczkamats() {
        List<Paczkamat> paczkamats = new ArrayList<>();

        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            paczkamats = (List<Paczkamat>) session.createQuery("FROM Paczkamat").list();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return paczkamats;
    }

    public List<Order> getAllOrders() {
        List<Order> order = new ArrayList<>();

        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            order = (List<Order>) session.createQuery("FROM Order").list();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return order;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();

        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            customers = (List<Customer>) session.createQuery("FROM Customer").list();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return customers;
    }

    public List<Stash> getAllStashes() {
        List<Stash> stashes = new ArrayList<>();

        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            stashes = (List<Stash>) session.createQuery("FROM Stash ").list();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return stashes;
    }

    public Customer getLoggedInUser(String login, String password) throws HibernateException {
        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            return (Customer) session.createQuery("FROM Customer customer where " +
                    "customer.login like :login and customer.password like :password").
                    setParameter("login", login).setParameter("password", password).getSingleResult();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public void insertPaczkamat( Paczkamat paczkamat ) {
        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            session.save(paczkamat);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void insertStash( Stash stash ) {
        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            session.save(stash);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void insertOrder( Order order ) {
        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            session.save(order);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public void insertCustomer( Customer customer ) {
        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            session.save(customer);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public <T> void updateEntity(T entity) {
        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            session.update(entity);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
}