/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package my.insa.yong.model;

/**
 * Classe représentant un utilisateur du système
 * @author francois
 */
public class Utilisateur {
    
    private int id;
    private String surnom;
    private String pass;
    private int role; // Niveau d'accès administratif
    
    public Utilisateur() {
    }
    
    public Utilisateur(String surnom, String pass, int role) {
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }
    
    public Utilisateur(int id, String surnom, String pass, int role) {
        this.id = id;
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSurnom() {
        return surnom;
    }
    
    public void setSurnom(String surnom) {
        this.surnom = surnom;
    }
    
    public String getPass() {
        return pass;
    }
    
    public void setPass(String pass) {
        this.pass = pass;
    }
    
    public int getRole() {
        return role;
    }
    
    public void setRole(int role) {
        this.role = role;
    }
    
    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", surnom='" + surnom + '\'' +
                ", role=" + role +
                '}';
    }
}