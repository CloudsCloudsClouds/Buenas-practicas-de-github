byte estadox = 125;
byte estadoy = 125;
const int mot_der_ade = 3;
const int mot_der_atr = 5;
const int mot_izq_ade = 6;
const int mot_izq_atr = 9;
byte velder;
byte velizq;
const unsigned long timeoutComandoMs = 350;
unsigned long ultimoComandoMs = 0;

void detenerMotores() {
  analogWrite(mot_der_ade, 0);
  analogWrite(mot_der_atr, 0);
  analogWrite(mot_izq_ade, 0);
  analogWrite(mot_izq_atr, 0);
}

void setup() {
  Serial.begin(9600);  
  pinMode(3,OUTPUT);
  pinMode(5,OUTPUT);
  pinMode(6,OUTPUT);
  pinMode(9,OUTPUT);
  analogWrite(mot_der_ade, 0);
  analogWrite(mot_der_atr, 0);
  analogWrite(mot_izq_ade, 0);
  analogWrite(mot_izq_atr, 0);
}

void loop() {
  while (Serial.available() >= 2) {
    estadox = Serial.read();
    delay(10);
    estadoy = Serial.read();
    ultimoComandoMs = millis();
    if (estadox > 250 || estadoy > 250) {
      estadox = 125;
      estadoy = 125;
    }
    /*Serial.print(estadox);
    Serial.print(",");
    Serial.println(estadoy);*/
  }
  if (millis() - ultimoComandoMs > timeoutComandoMs) {
    estadox = 125;
    estadoy = 125;
    detenerMotores();
    delay(10);
    return;
  }
  if(estadoy >= 83 && estadoy <= 166 && estadox >= 83 && estadox <= 166){ // Detenerse
    //Serial.println("Detenerse");
    analogWrite(mot_der_ade, 0);
    analogWrite(mot_der_atr, 0);
    analogWrite(mot_izq_ade, 0);
    analogWrite(mot_izq_atr, 0);
  }
  if(estadoy > 166 && estadoy <= 250 && estadox > 83 && estadox <  166){ // Atras
    //Serial.println("Atras");
    velder = map(estadoy, 166, 250, 0, 250);
    velizq = map(estadoy, 166, 250, 0, 250);
    analogWrite(mot_der_ade, 0);
    analogWrite(mot_der_atr, velder);
    analogWrite(mot_izq_ade, 0);
    analogWrite(mot_izq_atr, velizq);
  }
  if(estadoy < 83 && estadoy >= 0 && estadox > 83 && estadox <  166){ // Adelante
    //Serial.println("Adelante");
    velder = map(estadoy, 83, 0, 0, 250);
    velizq = map(estadoy, 83, 0, 0, 250);
    analogWrite(mot_der_ade, velder);
    analogWrite(mot_der_atr, 0);
    analogWrite(mot_izq_ade, velizq);
    analogWrite(mot_izq_atr, 0);
  }
  if(estadoy < 166 && estadoy > 83 && estadox >= 0 && estadox < 83){ // Izquierda
    //Serial.println("Izquierda");
    velder = map(estadox, 83, 0, 0, 250);
    velizq = map(estadox, 83, 0, 0, 250);
    analogWrite(mot_der_ade, velder);
    analogWrite(mot_der_atr, 0);
    analogWrite(mot_izq_ade, 0);
    analogWrite(mot_izq_atr, velizq);
  }
  if(estadoy < 166 && estadoy > 83 && estadox > 166 && estadox <= 250){ // Derecha
    //Serial.println("Derecha");
    velder = map(estadox, 166, 250, 0, 250);
    velizq = map(estadox, 166, 250, 0, 250);
    analogWrite(mot_der_ade, 0);
    analogWrite(mot_der_atr, velder);
    analogWrite(mot_izq_ade, velizq);
    analogWrite(mot_izq_atr, 0);
  }
  if(estadoy < 83 && estadoy >= 0 && estadox > 166 && estadox <= 250){ // Adelante Derecha
    //Serial.println("Adelante Derecha");
    velder = map(estadoy, 83, 0, 0, 250);
    velizq = map(estadox, 166, 250, 0, 250);
    analogWrite(mot_der_ade, velder/2);
    analogWrite(mot_der_atr, 0);
    analogWrite(mot_izq_ade, velizq);
    analogWrite(mot_izq_atr, 0);
  }
  if(estadoy <= 250 && estadoy > 166 && estadox > 166 && estadox <= 250){ // Atras Derecha
    //Serial.println("Atras Derecha");
    velder = map(estadoy, 166, 250, 0, 250);
    velizq = map(estadox, 166, 250, 0, 250);
    analogWrite(mot_der_ade, 0);
    analogWrite(mot_der_atr, velder/2);
    analogWrite(mot_izq_ade, 0);
    analogWrite(mot_izq_atr, velizq);
  }
  if(estadoy <= 250 && estadoy > 166 && estadox >= 0 && estadox < 83){ // Atras Izquierda
    //Serial.println("Atras Izquierda");
    velder = map(estadoy, 166, 250, 0, 250);
    velizq = map(estadox, 83, 0, 0, 250);
    analogWrite(mot_der_ade, 0);
    analogWrite(mot_der_atr, velder);
    analogWrite(mot_izq_ade, 0);
    analogWrite(mot_izq_atr, velizq/2);
  }
  if(estadoy < 83 && estadoy >= 0 && estadox >= 0 && estadox < 83){ // Adelante Izquierda
    //Serial.println("Adelante Izquierda");
    velder = map(estadoy, 83, 0, 0, 250);
    velizq = map(estadox, 83, 0, 0, 250);
    analogWrite(mot_der_ade, velder);
    analogWrite(mot_der_atr, 0);
    analogWrite(mot_izq_ade, velizq/2);
    analogWrite(mot_izq_atr, 0);
  }
  delay(10);
}
