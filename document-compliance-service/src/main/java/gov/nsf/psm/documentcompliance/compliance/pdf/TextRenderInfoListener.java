package gov.nsf.psm.documentcompliance.compliance.pdf;

import java.util.HashSet;
import java.util.Set;

import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

public class TextRenderInfoListener implements IEventListener {
    private final IEventListener strategy;
    private long blockCount;
    private float totalSingleSpaceWidth = 0f;
    private float averageSingleSpaceWidth = 0f;
    private double nextMaxY;
    private double prevMaxY;
    private boolean lastBlockOnSameLine;

    public TextRenderInfoListener(IEventListener strategy) {
        this.strategy = strategy;
    }

    private void renderText(TextRenderInfo renderInfo) {
        for (TextRenderInfo info : renderInfo.getCharacterRenderInfos()) {
            strategy.eventOccurred(info, EventType.RENDER_TEXT);
        }
        blockCount++;
        totalSingleSpaceWidth = totalSingleSpaceWidth + renderInfo.getSingleSpaceWidth();
        averageSingleSpaceWidth = totalSingleSpaceWidth/blockCount;
        prevMaxY = nextMaxY;
        nextMaxY = renderInfo.getBaseline().getBoundingRectangle().getY();
        lastBlockOnSameLine = Math.round(prevMaxY) == Math.round(nextMaxY);
    }

    public long getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(long blockCount) {
        this.blockCount = blockCount;
    }
    
    public float getAverageSingleSpaceWidth() {
        return averageSingleSpaceWidth;
    }

    public void setAverageSingleSpaceWidth(float averageSingleSpaceWidth) {
        this.averageSingleSpaceWidth = averageSingleSpaceWidth;
    }
    
    public boolean isLastBlockOnSameLine() {
        return lastBlockOnSameLine;
    }

    public void setLastBlockOnSameLine(boolean lastBlockOnSameLine) {
        this.lastBlockOnSameLine = lastBlockOnSameLine;
    }

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (!type.equals(EventType.RENDER_TEXT))
            return;
        renderText((TextRenderInfo) data); // Render text
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        HashSet<EventType> supportedEvents = new HashSet<>();
        supportedEvents.add(EventType.RENDER_TEXT);
        return supportedEvents;
    }

}