package fr.agroscan.service;

public class CurrentPasswordInvalidException extends RuntimeException {
    public CurrentPasswordInvalidException() {
        super("Le mot de passe actuel est incorrect");
    }
}
