package peoplesoft.model;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javafx.collections.ObservableList;
import peoplesoft.commons.util.JsonUtil;
import peoplesoft.model.job.Job;
import peoplesoft.model.job.JobList;
import peoplesoft.model.job.JobListManager;
import peoplesoft.model.person.Person;
import peoplesoft.model.person.UniquePersonList;

/**
 * Wraps all data at the address-book level
 * Duplicates are not allowed (by .isSamePerson comparison)
 */
@JsonSerialize(using = AddressBook.AddressBookSerializer.class)
@JsonDeserialize(using = AddressBook.AddressBookDeserializer.class)
public class AddressBook implements ReadOnlyAddressBook {

    private final UniquePersonList persons;
    private JobList jobs;

    /**
     * Creates an AddressBook using the Persons in the {@code toBeCopied}
     */
    public AddressBook() {
        persons = new UniquePersonList();
        jobs = new JobListManager();
    }

    /**
     * Creates an AddressBook using the Persons in the {@code toBeCopied}
     */
    public AddressBook(ReadOnlyAddressBook toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    /**
     * Creates an {@code AddressBook} using the given {@code UniquePersonList}. Only used by
     * {@code AddressBookDeserializer}.
     *
     * @param upl the {@code UniquePersonList} for the new instance
     */
    private AddressBook(UniquePersonList upl) {
        persons = upl;
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the person list with {@code persons}.
     * {@code persons} must not contain duplicate persons.
     */
    public void setPersons(List<Person> persons) {
        this.persons.setPersons(persons);
    }

    /**
     * Replaces the contents of the job list with {@code jobs}.
     * {@code jobs} must not contain duplicate jobs.
     */
    public void setJobs(List<Job> jobs) {
        this.jobs.setJobs(jobs);
    }

    /**
     * Resets the existing data of this {@code AddressBook} with {@code newData}.
     */
    public void resetData(ReadOnlyAddressBook newData) {
        requireNonNull(newData);

        setPersons(newData.getPersonList());
        // TODO: handle jobs - serdes issue
    }

    //// person-level operations

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book.
     */
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return persons.contains(person);
    }

    /**
     * Adds a person to the address book.
     * The person must not already exist in the address book.
     */
    public void addPerson(Person p) {
        persons.add(p);
    }


    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the address book.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the address book.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireNonNull(editedPerson);

        persons.setPerson(target, editedPerson);
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removePerson(Person key) {
        persons.remove(key);
    }

    //// job-level operations

    /**
     * Returns true if a job with the same identity as {@code job} exists in the address book.
     */
    public boolean hasJob(Job job) {
        requireNonNull(job);
        return jobs.contains(job);
    }

    /**
     * Adds a job to the address book.
     * The job must not already exist in the address book.
     */
    public void addJob(Job job) {
        jobs.add(job);
    }

    /**
     * Replaces the given job {@code target} in the list with {@code editedJob}.
     * {@code target} must exist in the address book.
     * The job identity of {@code editedJob} must not be the same as another existing job in the address book.
     */
    public void setJob(Job target, Job editedJob) {
        requireNonNull(editedJob);

        jobs.setJob(target, editedJob);
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removeJob(Job key) {
        jobs.remove(key);
    }

    //// util methods

    @Override
    public String toString() {
        return persons.asUnmodifiableObservableList().size() + " persons";
        // TODO: refine later
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return persons.asUnmodifiableObservableList();
    }

    @Override
    public ObservableList<Job> getJobList() {
        return jobs.asUnmodifiableObservableList();
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof AddressBook // instanceof handles nulls
                && persons.equals(((AddressBook) other).persons));
                // && jobs.equals(((AddressBook) other).jobs));
                // TODO: fix serdes error
    }

    @Override
    public int hashCode() {
        return Objects.hash(persons, jobs);
    }

    protected static class AddressBookSerializer extends StdSerializer<AddressBook> {
        private AddressBookSerializer(Class<AddressBook> val) {
            super(val);
        }

        private AddressBookSerializer() {
            this(null);
        }

        @Override
        public void serialize(AddressBook val, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            gen.writeObjectField("persons", val.persons);

            gen.writeEndObject();
        }
    }

    protected static class AddressBookDeserializer extends StdDeserializer<AddressBook> {
        private static final String MISSING_OR_INVALID_VALUE = "This address book is invalid!";

        private AddressBookDeserializer(Class<?> vc) {
            super(vc);
        }

        private AddressBookDeserializer() {
            this(null);
        }

        private static JsonNode getNonNullNode(ObjectNode node, String key, DeserializationContext ctx)
                throws JsonMappingException {
            JsonNode jsonNode = node.get(key);
            if (jsonNode == null) {
                throw JsonUtil.getWrappedIllegalValueException(
                    ctx, String.format(MISSING_OR_INVALID_VALUE, key));
            }

            return jsonNode;
        }

        @Override
        public AddressBook deserialize(JsonParser p, DeserializationContext ctx)
                throws IOException, JsonProcessingException {
            JsonNode node = p.readValueAsTree();
            ObjectCodec codec = p.getCodec();

            if (!(node instanceof ObjectNode)) {
                throw JsonUtil.getWrappedIllegalValueException(ctx, MISSING_OR_INVALID_VALUE);
            }

            ObjectNode objNode = (ObjectNode) node;

            UniquePersonList upl = codec.treeToValue(
                getNonNullNode(objNode, "persons", ctx),
                UniquePersonList.class);

            AddressBook ab = new AddressBook(upl);
            return ab;
        }

        @Override
        public AddressBook getNullValue(DeserializationContext ctx) throws JsonMappingException {
            throw JsonUtil.getWrappedIllegalValueException(ctx, MISSING_OR_INVALID_VALUE);
        }
    }
}
