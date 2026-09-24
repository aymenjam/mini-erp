package org.j2ra.minierp.customers.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A customer of the company that uses the ERP, with its contacts and addresses. Immutable: a change returns a new object.
 * All business rules are in the constructor, so an invalid customer cannot exist (also when it is read from the database).
 */
public record Customer(
        UUID id,
        CustomerType type,
        String name,
        String email,
        String phone,
        String vatNumber,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<Contact> contacts,
        List<Address> addresses) {

    public static final int NAME_MAX = 200;
    public static final int EMAIL_MAX = 254;
    public static final int PHONE_MAX = 50;
    public static final int VAT_NUMBER_MAX = 50;

    // A fixed order, so the same customer always shows its lists the same way.
    private static final Comparator<Contact> CONTACT_ORDER =
            Comparator.comparing(Contact::lastName).thenComparing(Contact::firstName).thenComparing(Contact::id);
    private static final Comparator<Address> ADDRESS_ORDER =
            Comparator.comparing(Address::type).thenComparing(Address::city).thenComparing(Address::id);

    public Customer {
        if (id == null || status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("id, status, createdAt and updatedAt must not be null");
        }
        if (type == null) {
            throw new InvalidCustomerException("type", "must not be null");
        }
        name = Fields.required("name", name, NAME_MAX);
        email = Fields.clean(email);
        phone = Fields.clean(phone);
        // Same VAT number written in two ways must not become two customers: we trim and use upper case.
        vatNumber = Fields.cleanUpper(vatNumber);
        Fields.tooLong("email", email, EMAIL_MAX);
        Fields.tooLong("phone", phone, PHONE_MAX);
        Fields.tooLong("vatNumber", vatNumber, VAT_NUMBER_MAX);
        if (vatNumber != null && type != CustomerType.COMPANY) {
            throw new InvalidCustomerException("vatNumber", "only a company can have a VAT number");
        }
        contacts = contacts == null ? List.of() : contacts.stream().sorted(CONTACT_ORDER).toList();
        addresses = addresses == null ? List.of() : addresses.stream().sorted(ADDRESS_ORDER).toList();
        if (contacts.stream().filter(Contact::primary).count() > 1) {
            throw new InvalidCustomerException("contacts", "only one contact can be the primary contact");
        }
        for (AddressType addressType : AddressType.values()) {
            if (addresses.stream().filter(a -> a.type() == addressType && a.defaultForType()).count() > 1) {
                throw new InvalidCustomerException("addresses", "only one " + addressType + " address can be the default");
            }
        }
    }

    /** A new active customer with a new random id (UUID v4), without contacts and addresses. */
    public static Customer create(CustomerType type, String name, String email, String phone, String vatNumber, Instant now) {
        return new Customer(UUID.randomUUID(), type, name, email, phone, vatNumber, CustomerStatus.ACTIVE, now, now, 0,
                List.of(), List.of());
    }

    /** Replaces the main fields. An inactive customer must be reactivated first. */
    public Customer update(CustomerType newType, String newName, String newEmail, String newPhone, String newVatNumber, Instant now) {
        requireActive();
        return new Customer(id, newType, newName, newEmail, newPhone, newVatNumber, status, createdAt, now, version,
                contacts, addresses);
    }

    /** Deactivating an inactive customer changes nothing. */
    public Customer deactivate(Instant now) {
        return status == CustomerStatus.INACTIVE ? this : withStatus(CustomerStatus.INACTIVE, now);
    }

    /** Reactivating an active customer changes nothing. */
    public Customer reactivate(Instant now) {
        return status == CustomerStatus.ACTIVE ? this : withStatus(CustomerStatus.ACTIVE, now);
    }

    /** Adds a contact. If it is primary, the other contacts are not primary any more. */
    public Customer addContact(Contact contact, Instant now) {
        requireActive();
        List<Contact> changed = new ArrayList<>(contacts);
        changed.add(contact);
        return withContacts(demoteOthers(changed, contact), now);
    }

    /** Replaces the contact that has the same id. If it is primary, the other contacts are not primary any more. */
    public Customer replaceContact(Contact contact, Instant now) {
        requireActive();
        findContact(contact.id());
        List<Contact> changed = new ArrayList<>(contacts.stream().filter(c -> !c.id().equals(contact.id())).toList());
        changed.add(contact);
        return withContacts(demoteOthers(changed, contact), now);
    }

    public Customer removeContact(UUID contactId, Instant now) {
        requireActive();
        findContact(contactId);
        return withContacts(contacts.stream().filter(c -> !c.id().equals(contactId)).toList(), now);
    }

    /** Adds an address. If it is the default, the other addresses of the same type are not the default any more. */
    public Customer addAddress(Address address, Instant now) {
        requireActive();
        List<Address> changed = new ArrayList<>(addresses);
        changed.add(address);
        return withAddresses(demoteOthers(changed, address), now);
    }

    /** Replaces the address that has the same id. If it is the default, the others of the same type are not the default any more. */
    public Customer replaceAddress(Address address, Instant now) {
        requireActive();
        findAddress(address.id());
        List<Address> changed = new ArrayList<>(addresses.stream().filter(a -> !a.id().equals(address.id())).toList());
        changed.add(address);
        return withAddresses(demoteOthers(changed, address), now);
    }

    public Customer removeAddress(UUID addressId, Instant now) {
        requireActive();
        findAddress(addressId);
        return withAddresses(addresses.stream().filter(a -> !a.id().equals(addressId)).toList(), now);
    }

    private void requireActive() {
        if (status == CustomerStatus.INACTIVE) {
            throw new CustomerInactiveException(id);
        }
    }

    private Contact findContact(UUID contactId) {
        return contacts.stream().filter(c -> c.id().equals(contactId)).findFirst()
                .orElseThrow(() -> new ContactNotFoundException(contactId));
    }

    private Address findAddress(UUID addressId) {
        return addresses.stream().filter(a -> a.id().equals(addressId)).findFirst()
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    private static List<Contact> demoteOthers(List<Contact> all, Contact changed) {
        if (!changed.primary()) {
            return all;
        }
        return all.stream().map(c -> c.id().equals(changed.id()) ? c : c.withPrimary(false)).toList();
    }

    private static List<Address> demoteOthers(List<Address> all, Address changed) {
        if (!changed.defaultForType()) {
            return all;
        }
        return all.stream()
                .map(a -> a.id().equals(changed.id()) || a.type() != changed.type() ? a : a.withDefaultForType(false))
                .toList();
    }

    private Customer withStatus(CustomerStatus newStatus, Instant now) {
        return new Customer(id, type, name, email, phone, vatNumber, newStatus, createdAt, now, version, contacts, addresses);
    }

    private Customer withContacts(List<Contact> newContacts, Instant now) {
        return new Customer(id, type, name, email, phone, vatNumber, status, createdAt, now, version, newContacts, addresses);
    }

    private Customer withAddresses(List<Address> newAddresses, Instant now) {
        return new Customer(id, type, name, email, phone, vatNumber, status, createdAt, now, version, contacts, newAddresses);
    }
}
