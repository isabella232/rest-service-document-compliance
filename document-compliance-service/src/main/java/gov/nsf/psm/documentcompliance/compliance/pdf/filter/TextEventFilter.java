package gov.nsf.psm.documentcompliance.compliance.pdf.filter;

import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.filter.IEventFilter;

public class TextEventFilter implements IEventFilter {

    @Override
    public boolean accept(IEventData data, EventType type) {
        if (!type.equals(EventType.RENDER_TEXT))
            return false;
        TextRenderInfo renderInfo = (TextRenderInfo) data;
        return renderInfo != null && renderInfo.getText().trim().length() > 0;
    }

}
