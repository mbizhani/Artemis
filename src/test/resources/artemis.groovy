import org.devocative.artemis.Context

import java.text.DecimalFormat;

// Called by ContextHandler
def init(Context ctx) {
    if (ctx.profile == "dev") {
        ctx.baseUrl = "http://backend-org-kyc-bmi.borna-dev"
    }
}

def generator(String alphabet, int n) {
    new Random().with {
        (1..n).collect { alphabet[nextInt(alphabet.length())] }.join()
    }
}

def initCustomer(Context ctx) {
    // Generate 9 random digits
    int randomNumber = (int) Math.floor(Math.random() * 1000000000)
    DecimalFormat formatNumber = new DecimalFormat("000000000")
    String randomNumberString = formatNumber.format(randomNumber)
    char[] randomNumberArray = randomNumberString.toCharArray()
    int arrayLength = randomNumberArray.length

    // Reverse the array.
    for (int i = 0; i < arrayLength / 2; i++) {
        char swap = randomNumberArray[i]
        randomNumberArray[i] = randomNumberArray[arrayLength - i - 1]
        randomNumberArray[arrayLength - i - 1] = swap
    }

    // Calculate Control Code.
    int sum = 0;
    for (int i = 0; i < arrayLength; i++) {
        sum += Character.getNumericValue(randomNumberArray[i]) * (i + 2)
    }
    int remainder = sum % 11
    int controlCode = remainder >= 2 ? 11 - remainder : remainder

    // Append Control Code to end of generated number
    String nationalID = randomNumberString + Integer.toString(controlCode)

    ctx.addVar("nationalID", nationalID)
    ctx.addVar("mobile", "09${generator(('0'..'9').join(), 9)}")
}
