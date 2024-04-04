package studio.magemonkey.fusion.queue;

import studio.magemonkey.fusion.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
public class QueueItem {

    @Getter
    @Setter
    private @NonNull Recipe recipe;

    @Getter
    private int amount;

    @Getter
    private boolean done;


    public void attemptCraft() {
        if (amount > 0) {

            //Craft the item.

        }

        done = amount == 0;

    }

    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IndexOutOfBoundsException("Amount cannot be less than 0.");
        }


    }

}
