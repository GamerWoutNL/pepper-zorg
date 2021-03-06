export enum Sender {
    STAFF,
    PLATFORM
}

export enum Person {
    PATIENT,
    NONE
}

export enum Task {
    USER,
    
    FEEDBACK,
    FEEDBACK_ID,
    FEEDBACK_STATUS,
    FEEDBACK_EXPLANATION,
    FEEDBACK_TIMESTAMP,

    MEAL,
    MEAL_ID,
    MEAL_NAME,
    MEAL_DESCRIPTION,
    MEAL_CALORIES,
    MEAL_ALLERGIES,
    MEAL_IMAGE,

    MEAL_ORDER,
    MEAL_ORDER_ID,
    MEAL_ORDER_MEAL_ID,
    MEAL_ORDER_TIMESTAMP,

    ANSWER,
    ANSWER_ID,
    ANSWER_TEXT,
    ANSWER_QUESTION_ID,
    ANSWER_TIMESTAMP,

    QUESTION,
    QUESTION_ID,
    QUESTION_TEXT,
    QUESTION_TIMESTAMP,

    REMINDER,
    REMINDER_ID,
    REMINDER_THING,
    REMINDER_TIMESTAMP,

    PATIENT,
    PATIENT_ID,
    PATIENT_NAME,
    PATIENT_BIRTHDATE,
    PATIENT_ALLERGIES
}

export interface Message {
    sender: Sender;
    senderId: string;
    person: Person;
    personId: string;
    task: Task,
    taskId: string;
    data: string;
}
